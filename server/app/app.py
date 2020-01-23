from __future__ import division
from __future__ import print_function

import sys
import cv2
import editdistance
from gingerit.gingerit import GingerIt
from DataLoader import DataLoader, Batch
from Model import Model, DecoderType
from SamplePreprocessor import preprocess
from flask import Flask, render_template, request, jsonify, send_file
from werkzeug import secure_filename
from werkzeug.datastructures import ImmutableMultiDict
import numpy as np
import os
import subprocess
from WordSegmentation import wordSegmentation, prepareImg, lineSegmentation

app = Flask(__name__)
parser = GingerIt()

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
        fulltxt = ""
        f = request.files["newimage"]
        f.save(secure_filename(f.filename))
        originImage = cv2.imread(f.filename)
        cropImg = lineSegmentation(originImage)
        j=0
        for (n,I) in enumerate(cropImg):   
            img_read = I

            if img_read.shape[0] < 70:
                scale = 3.53 
                width = int(img_read.shape[1] * scale)
                height = int(img_read.shape[0] * scale)
                dim = (width, height)
                # resize image
                img_read = cv2.resize(img_read, dim, interpolation = cv2.INTER_AREA)

            nb_pixel = img_read.shape[0]*img_read.shape[1]
            white_percent = np.count_nonzero(img_read-255)/(img_read.shape[0]*img_read.shape[1])
            ratio_h_l = img_read.shape[0]/img_read.shape[1]

            prepare_param = nb_pixel * white_percent * ratio_h_l * 0.015
            img = prepareImg(img_read,prepare_param)

            res = wordSegmentation(img, kernelSize=25, sigma=11, theta=7, minArea=100)
            res = sorted(res, key = lambda x:x[2])

            for (x,m) in enumerate(res):
                (wordBox, wordImg, xpos, ypos) = m
                (x, y, w, h) = wordBox
                cv2.imwrite('data/%d.jpg'%j, wordImg) # save word
                prediction = infer(model, 'data/%d.jpg'%j)
                fulltxt += " " + prediction
                j = j+1

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

