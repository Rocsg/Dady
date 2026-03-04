package io.github.rocsg.dady.utils;

import ij.IJ;
import ij.ImagePlus;
import ij.plugin.Duplicator;
import io.github.rocsg.fijiyama.common.VitimageUtils;

public class IconicUtils {
    public static void main(String[] args) {
        
    }

    public static ImagePlus generateNdvi(ImagePlus img){
        ImagePlus imgR=new Duplicator().run(img,3,3);
        ImagePlus imgIR=new Duplicator().run(img,5,5);
        ImagePlus imgNdvi=generateNdvi(imgR,imgIR);
        return imgNdvi;
    }


    public static ImagePlus generateNdvi(ImagePlus imgRt,ImagePlus imgIRt){
        ImagePlus imgR=VitimageUtils.convertShortToFloatWithoutDynamicChanges(imgRt);
        ImagePlus imgIR=VitimageUtils.convertShortToFloatWithoutDynamicChanges(imgIRt);
        ImagePlus imgSub=VitimageUtils.makeOperationBetweenTwoImages(imgIR, imgR, 4, true);
        ImagePlus imgAdd=VitimageUtils.makeOperationBetweenTwoImages(imgIR, imgR, 1, true);
        ImagePlus imgNdvi=VitimageUtils.makeOperationBetweenTwoImages(imgSub, imgAdd, 3, true);
        imgNdvi.setDisplayRange(-1, 1);
        VitimageUtils.adjustImageCalibration(imgNdvi, imgR);
        return imgNdvi;    
    }
}
