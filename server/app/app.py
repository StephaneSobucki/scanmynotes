from __future__ import division
from __future__ import print_function

import sys
import cv2
import editdistance
from DataLoader import DataLoader, Batch
from Model import Model, DecoderType
from SamplePreprocessor import preprocess
from flask import Flask, render_template, request, jsonify, send_file
from werkzeug import secure_filename
from werkzeug.datastructures import ImmutableMultiDict
import numpy as np
import os
import subprocess

app = Flask(__name__)

class FilePaths:
    fnCharList = '../model/charList.txt'
    fnAccuracy = '../model/accuracy.txt'
    fnTrain = '../data/'
    fnInfer = '../data/test.png'
    fnCorpus = '../data/corpus.txt'


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
        f = request.files["newimage"]
        f.save(secure_filename(f.filename))
        prediction = infer(model,f.filename)
        os.remove(f.filename)
        File_object = open("file.txt","w")
        File_object.write(prediction)
        File_object.close()
        subprocess.run(['python','txt2pdf.py','file.txt'])
    #return jsonify({'Predicted Class':prediction})
    return send_file('/var/www/scanmynotes/app/output.pdf')
	
if __name__ == '__main__':
    app.run(host = '0.0.0.0',debug = False)

