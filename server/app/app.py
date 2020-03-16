from __future__ import division
from __future__ import print_function

import sys
import cv2
from DataLoader import DataLoader, Batch
from Model import Model, DecoderType
from flask import Flask, render_template, request, jsonify, send_file
from werkzeug.utils import secure_filename
from werkzeug.datastructures import ImmutableMultiDict
from SamplePreprocessor import preprocess
from gingerit.gingerit import GingerIt
import numpy as np
import os
import subprocess

app = Flask(__name__)
parser = GingerIt()

def infer(model, fnImg):
    img = preprocess(cv2.imread(fnImg, cv2.IMREAD_GRAYSCALE), Model.imgSize)
    batch = Batch(None, [img])
    (recognized, probability) = model.inferBatch(batch, True)
    return recognized[0]

decoderType = DecoderType.BestPath
model = Model(open('charList.txt').read(), decoderType, mustRestore=True)
 
@app.route('/', methods = ['POST'])
def upload_file():
    if request.method == 'POST':
        fulltxt = ""
        f = request.files["newimage"]
        f.save(secure_filename(f.filename))
        subprocess.run(["text_segmentation/bin/./text_segmentation",f.filename,f.filename+"d"])
        words = os.listdir(f.filename+"d/words")
        words.sort()

        for word in words:
            prediction = infer(model, f.filename+"d/words/"+word)
            fulltxt += " " + prediction

    fulltxt = parser.parse(fulltxt)['result']

    return jsonify({'Predicted Class':fulltxt})

@app.route('/PDF', methods = ['POST'])
def download_pdf():
    if request.method == 'POST':
        data = request.get_data(as_text = True)
        data = data.split("\"")[2].split("\\")[0]
        File_object = open("file.txt","w")
        File_object.write(data)
        File_object.close()
        subprocess.run(['python','txt2pdf.py','file.txt'])
        return send_file('/var/www/scanmynotes/app/output.pdf')
	
if __name__ == '__main__':
    app.run(host = '0.0.0.0',debug = False)

