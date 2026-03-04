package io.github.rocsg.dady.preprocessing;

import java.awt.Point;

import ij.IJ;
import ij.ImageJ;
import ij.ImagePlus;
import ij.plugin.frame.RoiManager;
import io.github.rocsg.dady.preprocessing.sergio.Step1_FullRasterRegistration;
import io.github.rocsg.dady.utils.DataHandling;
import io.github.rocsg.dady.utils.IconicUtils;
import io.github.rocsg.dady.utils.RegistrationUtils;
import io.github.rocsg.fijiyama.common.VitimageUtils;
import io.github.rocsg.fijiyama.fijiyamaplugin.RegistrationAction;
import io.github.rocsg.fijiyama.registration.BlockMatchingRegistration;
import io.github.rocsg.fijiyama.registration.ItkTransform;
import io.github.rocsg.fijiyama.registration.MetricType;
import io.github.rocsg.fijiyama.registration.Transform3DType;
import io.github.rocsg.fijiyama.registration.TransformUtils;
import math3d.Point3d;

//This class works on the output of Step1_FullRasterRegistration
//From the aligned full raster (up to a precision of 15 cm),
//It complete the alignment of the semi-raster comprising the 9 rows of 1500 genotypes, up to a precision of 5 cm.
//Step by step, it is a cropping of the 5 images and a registration with Block Matching.

//The code is written in Java/IMageJ for visualisation purpose, and to foster registration automatic scheme
public class Step2_SemiRasterRegistration {

    public static final String mainDir= DataHandling.getUserPreprocessingDataPath();
    public static final String[]names=DataHandling.getObservationDates();
    static double ratioFactorForSigmaComputation=15;

    //The coordinates of the crop of the semi-raster, in the shape of the reference time 2024_2_27_Andrano.tif
    public static final int x0=3160;
    public static final int y0=2250;
    public static final int x1=10839;
    public static final int y1=9545;
    
    public static void main(String[] args) {
        ImageJ ij=new ImageJ();
        String []names=Step1_FullRasterRegistration.names;

        //Step  2.1 is the preparation of the semi-raster, the computation of RGB ndvi and grayscale
        if(false){
            doTheCropsOfHalfTheField();
            generateRGB();
            generateNdvi();
            generateGrayscale();
        }

        //Step 2.2 is the registration of the semi-raster using manual annotations
        if(false){
            annotateSemiRaster();
            registerWithLandmarksSemiRaster();
        }
         

        //Step 2.3 is the registration of the semi-raster using automatic registration
        if(false){
            computeNdviAndMeanFusRegistered();
        }
        if(false){
            registerWithBlockMatchingSemiRaster(1);
            registerWithBlockMatchingSemiRaster(2);
            registerWithBlockMatchingSemiRaster(3);
            registerWithBlockMatchingSemiRaster(4);
        }

        //Step 2.4 is the second step of registration of the semi-raster using automatic registration on the fused ndvi
        if(false){
            computeNdviAndMeanFusRegisteredAutoStep1();
        }
        if(false){
            registerWithBlockMatchingSemiRasterStep2(3);
            registerWithBlockMatchingSemiRasterStep2(1);
            registerWithBlockMatchingSemiRasterStep2(2);
            registerWithBlockMatchingSemiRasterStep2(4);
 
        }
        //Step 2.5 is the third step of registration of the semi-raster using automatic registration on the fused ndvi
        if(false){
            computeNdviAndMeanFusRegisteredAutoStep2();
        }
        if(false){
            registerWithBlockMatchingSemiRasterStep3(3);
            registerWithBlockMatchingSemiRasterStep3(1);
            registerWithBlockMatchingSemiRasterStep3(2);
            registerWithBlockMatchingSemiRasterStep3(4);
 
        }
        //Step 2.6 is the last step of registration of the semi-raster using automatic registration on the fused ndvi
        if(false){
            computeNdviAndMeanFusRegisteredAutoStep3();
        }
        if(false){
            registerWithBlockMatchingSemiRasterStep4(3);
            registerWithBlockMatchingSemiRasterStep4(1);
            registerWithBlockMatchingSemiRasterStep4(2);
            registerWithBlockMatchingSemiRasterStep4(4);
 
        }


        //Step 2.7 use the previous computed transform to generate the channels, and the rgb images resampled
        if(true){
            generateFinalChannels();
            generateFinalRGB();
        }
    }

public static void generateFinalRGB(){
    for(int i=0;i<5;i++){
        String name=names[i];
        ImagePlus img0=IJ.openImage(mainDir+"/SemiRaster/Registered/FinalChannels/"+name+"_channel_0.tif");
        ImagePlus img1=IJ.openImage(mainDir+"/SemiRaster/Registered/FinalChannels/"+name+"_channel_1.tif");
        ImagePlus img2=IJ.openImage(mainDir+"/SemiRaster/Registered/FinalChannels/"+name+"_channel_2.tif");
        img0.show();
        img1.show();
        img2.show();
        IJ.run(img0, "Merge Channels...", "c1="+name+"_channel_2.tif c2="+name+"_channel_1.tif c3="+name+"_channel_0.tif");
        ImagePlus img=IJ.getImage();
        IJ.saveAsTiff(img, mainDir+"/SemiRaster/Registered/FinalRGB/"+name+".tif");
        }
    }


    public static void generateFinalChannels(){
        for(int i=0;i<5;i++){
            System.out.println("Processing img "+i);
            String name=names[i];
            ItkTransform tr=null;
            ImagePlus[]channels=new ImagePlus[5];
            for(int j=0;j<5;j++){
                channels[j]=IJ.openImage(mainDir+"/SemiRaster/Raw/"+name+"_channel_"+j+".tif");
                VitimageUtils.adjustImageCalibration(channels[j], new double[]{1,1,1}, "cm");
            }

            if(i!=0 ){
                tr=ItkTransform.readTransformFromFile(mainDir+"/SemiRaster/Transformations/"+name+".transform.tif");
                System.out.println(mainDir+"/SemiRaster/Transformations/"+name+"_stepauto_step4.transform.tif");
                ItkTransform tr2=ItkTransform.readAsDenseField(mainDir+"/SemiRaster/Transformations/"+name+"_stepauto_step4.transform.tif");
                tr.addTransform(tr2);
                tr=tr.getFlattenDenseField(channels[0]);
                for(int j=0;j<5;j++){
                    channels[j] =tr.transformImage(channels[j], channels[j]);
                }
            }

            for(int j=0;j<5;j++){
                IJ.saveAsTiff(channels[j], mainDir+"/SemiRaster/Registered/FinalChannels/"+name+"_channel_"+j+".tif");
            }
        }
    }


    public static void registerWithBlockMatchingSemiRaster(int imgIndex){
        //Open the mean ndvi as reference
        ImagePlus imgRef=IJ.openImage(mainDir+"/SemiRaster/Registered/Ndvi_man/mean.tif");
        //Open the image to be registered
        ImagePlus imgMov=IJ.openImage(mainDir+"/SemiRaster/Registered/Ndvi_man/"+names[imgIndex]+".tif");
        //Do the registration
        ItkTransform tr=registerSemiRaster(imgRef, imgMov,null);
        //Save the result
        IJ.saveAsTiff(tr.transformImage(imgRef, imgMov), mainDir+"/SemiRaster/Registered/Ndvi_auto/"+names[imgIndex]+".tif");
        //Save the transformation
        tr.writeAsDenseField(mainDir+"/SemiRaster/Transformations/"+names[imgIndex]+"_stepauto.transform.tif", imgRef);
    }

    public static void registerWithBlockMatchingSemiRasterStep2(int imgIndex){
        //Open the mean ndvi as reference
        ImagePlus imgRef=IJ.openImage(mainDir+"/SemiRaster/Registered/Ndvi_auto/mean.tif");
        //Open the image to be registered
        ImagePlus imgMov=IJ.openImage(mainDir+"/SemiRaster/Registered/Ndvi_man/"+names[imgIndex]+".tif");
        //Do the registration
        ItkTransform trInit=ItkTransform.readAsDenseField(mainDir+"/SemiRaster/Transformations/"+names[imgIndex]+"_stepauto.transform.tif");
        ItkTransform tr=registerSemiRaster(imgRef, imgMov,trInit);
        //Save the result
        IJ.saveAsTiff(tr.transformImage(imgRef, imgMov), mainDir+"/SemiRaster/Registered/Ndvi_auto_step2/"+names[imgIndex]+".tif");
        //Save the transformation
        tr.writeAsDenseField(mainDir+"/SemiRaster/Transformations/"+names[imgIndex]+"_stepauto_step2.transform.tif", imgRef);
    }

    public static void registerWithBlockMatchingSemiRasterStep3(int imgIndex){
        //Open the mean ndvi as reference
        ImagePlus imgRef=IJ.openImage(mainDir+"/SemiRaster/Registered/Ndvi_auto_step2/mean.tif");
        //Open the image to be registered
        ImagePlus imgMov=IJ.openImage(mainDir+"/SemiRaster/Registered/Ndvi_man/"+names[imgIndex]+".tif");
        //Do the registration
        ItkTransform trInit=ItkTransform.readAsDenseField(mainDir+"/SemiRaster/Transformations/"+names[imgIndex]+"_stepauto_step2.transform.tif");
        ItkTransform tr=registerSemiRasterStep3(imgRef, imgMov,trInit);
        //Save the result
        IJ.saveAsTiff(tr.transformImage(imgRef, imgMov), mainDir+"/SemiRaster/Registered/Ndvi_auto_step3/"+names[imgIndex]+".tif");
        //Save the transformation
        tr.writeAsDenseField(mainDir+"/SemiRaster/Transformations/"+names[imgIndex]+"_stepauto_step3.transform.tif", imgRef);
    }

    public static void registerWithBlockMatchingSemiRasterStep4(int imgIndex){
        //Open the mean ndvi as reference
        ImagePlus imgRef=IJ.openImage(mainDir+"/SemiRaster/Registered/Ndvi_auto_step3/mean.tif");
        //Open the image to be registered
        ImagePlus imgMov=IJ.openImage(mainDir+"/SemiRaster/Registered/Ndvi_man/"+names[imgIndex]+".tif");
        //Do the registration
        ItkTransform trInit=ItkTransform.readAsDenseField(mainDir+"/SemiRaster/Transformations/"+names[imgIndex]+"_stepauto_step3.transform.tif");
        ItkTransform tr=registerSemiRasterStep4(imgRef, imgMov,trInit);
        //Save the result
        IJ.saveAsTiff(tr.transformImage(imgRef, imgMov), mainDir+"/SemiRaster/Registered/Ndvi_auto_step4/"+names[imgIndex]+".tif");
        //Save the transformation
        tr.writeAsDenseField(mainDir+"/SemiRaster/Transformations/"+names[imgIndex]+"_stepauto_step4.transform.tif", imgRef);
    }

    public static ImagePlus buildMaskFromNdviPair(ImagePlus img1,ImagePlus img2){
        //Create a binary mask of area that have value 0 in 32-bit
        ImagePlus img1_32=VitimageUtils.thresholdImageToFloatMask(img1, -0.00000001, 0.0000001);
        ImagePlus img2_32=VitimageUtils.thresholdImageToFloatMask(img2, -0.00000001, 0.0000001);
        ImagePlus imgMask=VitimageUtils.makeOperationBetweenTwoImages(img1_32, img2_32, 1, true);
        imgMask=VitimageUtils.thresholdImageToFloatMask(imgMask, -1, 0.9);
        imgMask=VitimageUtils.gaussianFiltering(imgMask, 100,100,0);
        imgMask=VitimageUtils.thresholdImage(imgMask, 0.8, 10);
        return imgMask;
    }




    public static ItkTransform registerSemiRaster(ImagePlus imgRef, ImagePlus imgMov,ItkTransform trInit){
        ImagePlus imgMask=buildMaskFromNdviPair(imgRef, imgMov);
        double sigma=imgRef.getWidth()*VitimageUtils.getVoxelSizes(imgRef)[0]/ratioFactorForSigmaComputation/1.5;//rien

        RegistrationAction regAct=new RegistrationAction();
        regAct.defineSettingsFromTwoImages(imgRef, imgMov);
        regAct.typeAction=RegistrationAction.TYPEACTION_AUTO;
        regAct.typeTrans=Transform3DType.DENSE;
        regAct.typeAutoDisplay=2;
        regAct.levelMaxDense=3;
        regAct.levelMinDense=2;
        regAct.iterationsBMDen=12;
        regAct.strideX=10;
        regAct.strideY=10;
//        regAct.neighX=3;
//        regAct.neighY=3;//rien
        regAct.bhsX=15;//7
        regAct.bhsY=15;//7
        regAct.sigmaDense=sigma;
        regAct.selectScore=90;//5
        regAct.selectRandom=90;
        regAct.selectLTS=70;
        
        BlockMatchingRegistration bm=BlockMatchingRegistration.setupBlockMatchingRegistration(imgRef, imgMov, regAct);

        bm.percentageBlocksSelectedByScore=99;
        bm.metricType=MetricType.SQUARED_CORRELATION;
        bm.mask=imgMask;
        bm.minBlockScore=0.2;//0.05
        bm.minBlockVariance=0.01;
        

        ItkTransform tr=bm.runBlockMatching(trInit, false);
        System.out.println("BM ok.");

        bm.closeLastImages();
        return tr;
    }

    public static ItkTransform registerSemiRasterStep3(ImagePlus imgRef, ImagePlus imgMov,ItkTransform trInit){
        ImagePlus imgMask=buildMaskFromNdviPair(imgRef, imgMov);
        double sigma=imgRef.getWidth()*VitimageUtils.getVoxelSizes(imgRef)[0]/ratioFactorForSigmaComputation/2;//rien

        RegistrationAction regAct=new RegistrationAction();
        regAct.defineSettingsFromTwoImages(imgRef, imgMov);
        regAct.typeAction=RegistrationAction.TYPEACTION_AUTO;
        regAct.typeTrans=Transform3DType.DENSE;
        regAct.typeAutoDisplay=0;
        regAct.levelMaxDense=3;
        regAct.levelMinDense=1;
        regAct.iterationsBMDen=8;
        regAct.strideX=10;
        regAct.strideY=10;
//        regAct.neighX=3;
//        regAct.neighY=3;//rien
        regAct.bhsX=9;//7
        regAct.bhsY=9;//7
        regAct.sigmaDense=sigma;
        regAct.selectScore=90;//5
        regAct.selectRandom=90;
        regAct.selectLTS=99;

        
        BlockMatchingRegistration bm=BlockMatchingRegistration.setupBlockMatchingRegistration(imgRef, imgMov, regAct);

        bm.percentageBlocksSelectedByVariance=99;
        bm.percentageBlocksSelectedByScore=80;
        bm.metricType=MetricType.SQUARED_CORRELATION;
        bm.mask=imgMask;
        bm.minBlockScore=0.2;//0.05
        bm.minBlockVariance=0.001;
        

        ItkTransform tr=bm.runBlockMatching(trInit, false);
        System.out.println("BM ok.");

        bm.closeLastImages();
        return tr;
    }

    public static ItkTransform registerSemiRasterStep4(ImagePlus imgRef, ImagePlus imgMov,ItkTransform trInit){
        ImagePlus imgMask=buildMaskFromNdviPair(imgRef, imgMov);
        double sigma=imgRef.getWidth()*VitimageUtils.getVoxelSizes(imgRef)[0]/ratioFactorForSigmaComputation/2.2;//rien

        RegistrationAction regAct=new RegistrationAction();
        regAct.defineSettingsFromTwoImages(imgRef, imgMov);
        regAct.typeAction=RegistrationAction.TYPEACTION_AUTO;
        regAct.typeTrans=Transform3DType.DENSE;
        regAct.typeAutoDisplay=0;
        regAct.levelMaxDense=3;
        regAct.levelMinDense=1;
        regAct.iterationsBMDen=14;
        regAct.strideX=10;
        regAct.strideY=10;
//        regAct.neighX=3;
//        regAct.neighY=3;//rien
        regAct.bhsX=9;//7
        regAct.bhsY=9;//7
        regAct.sigmaDense=sigma;
        regAct.selectScore=90;//5
        regAct.selectRandom=90;
        regAct.selectLTS=99;

        
        BlockMatchingRegistration bm=BlockMatchingRegistration.setupBlockMatchingRegistration(imgRef, imgMov, regAct);

        bm.percentageBlocksSelectedByVariance=99;
        bm.percentageBlocksSelectedByScore=80;
        bm.metricType=MetricType.SQUARED_CORRELATION;
        bm.mask=imgMask;
        bm.minBlockScore=0.2;//0.05
        bm.minBlockVariance=0.001;
        

        ItkTransform tr=bm.runBlockMatching(trInit, false);
        System.out.println("BM ok.");

        bm.closeLastImages();
        return tr;
    }

    public static void computeNdviAndMeanFusRegisteredAutoStep3(){
        ImagePlus []imgNdviTab=new ImagePlus[5];
        for(int i=0;i<5;i++){
            System.out.println("Processing img "+i);
            String name=names[i];           

            imgNdviTab[i]=IJ.openImage(mainDir+"/SemiRaster/Registered/Ndvi_auto_step3/"+name+".tif");
        }            
        ImagePlus imgNdviMean=VitimageUtils.meanOfImageArray(imgNdviTab);
        IJ.saveAsTiff(imgNdviMean, mainDir+"/SemiRaster/Registered/Ndvi_auto_step3/mean.tif");
    }

    public static void computeNdviAndMeanFusRegisteredAutoStep2(){
        ImagePlus []imgNdviTab=new ImagePlus[5];
        for(int i=0;i<5;i++){
            System.out.println("Processing img "+i);
            String name=names[i];           

            imgNdviTab[i]=IJ.openImage(mainDir+"/SemiRaster/Registered/Ndvi_auto_step2/"+name+".tif");
        }            
        ImagePlus imgNdviMean=VitimageUtils.meanOfImageArray(imgNdviTab);
        IJ.saveAsTiff(imgNdviMean, mainDir+"/SemiRaster/Registered/Ndvi_auto_step2/mean.tif");
    }

    public static void computeNdviAndMeanFusRegisteredAutoStep1(){
        ImagePlus []imgNdviTab=new ImagePlus[5];
        for(int i=0;i<5;i++){
            System.out.println("Processing img "+i);
            String name=names[i];           

            imgNdviTab[i]=IJ.openImage(mainDir+"/SemiRaster/Registered/Ndvi_auto/"+name+".tif");
        }            
        ImagePlus imgNdviMean=VitimageUtils.meanOfImageArray(imgNdviTab);
        IJ.saveAsTiff(imgNdviMean, mainDir+"/SemiRaster/Registered/Ndvi_auto/mean.tif");
    }


    public static void computeNdviAndMeanFusRegistered(){
        ImagePlus []imgNdviTab=new ImagePlus[5];
        for(int i=0;i<5;i++){
            System.out.println("Processing img "+i);
            String name=names[i];           

            imgNdviTab[i]=IJ.openImage(mainDir+"/SemiRaster/Ndvi/"+name+".tif");
            if(i>0){
                ItkTransform tr=ItkTransform.readTransformFromFile(mainDir+"/SemiRaster/Transformations/"+name+".transform.tif");
                imgNdviTab[i] =tr.transformImage(imgNdviTab[i], imgNdviTab[i]);
            }
            VitimageUtils.adjustImageCalibration(imgNdviTab[i], new double[]{1,1,1}, "cm");
            imgNdviTab[i]=VitimageUtils.subXYZ(imgNdviTab[i], new double[]{0.5,0.5,1}, true);
            IJ.saveAsTiff(imgNdviTab[i], mainDir+"/SemiRaster/Registered/Ndvi_man/"+name+".tif");
        }            
        ImagePlus imgNdviMean=VitimageUtils.meanOfImageArray(imgNdviTab);
        IJ.saveAsTiff(imgNdviMean, mainDir+"/SemiRaster/Registered/Ndvi_man/mean.tif");
    }


    public static void generateNdvi(){
        for(int i=0;i<5;i++){
            System.out.println("Processing img "+i);
            String name=names[i];           
            ImagePlus imgR=IJ.openImage(mainDir+"/SemiRaster/Raw/"+name+"_channel_"+2+".tif");
            ImagePlus imgIR=IJ.openImage(mainDir+"/SemiRaster/Raw/"+name+"_channel_"+4+".tif");
            ImagePlus imgNdvi=IconicUtils.generateNdvi(imgR,imgIR);
            IJ.saveAsTiff(imgNdvi, mainDir+"/SemiRaster/Ndvi/"+name+".tif");
        }
    }

    public static void generateGrayscale(){
        for(int i=0;i<5;i++){
            System.out.println("Processing img "+i);
            String name=names[i];           
            ImagePlus[]imgTab=new ImagePlus[5];
            for(int ch=0;ch<5;ch++){
                imgTab[ch]=IJ.openImage(mainDir+"/SemiRaster/Raw/"+name+"_channel_"+ch+".tif");
                imgTab[ch]=VitimageUtils.convertShortToFloatWithoutDynamicChanges (imgTab[ch]);
            }
            ImagePlus imgGray=VitimageUtils.meanOfImageArray(imgTab);
            IJ.saveAsTiff(imgGray, mainDir+"/SemiRaster/Grayscale/"+name+".tif");
        }
    }

    public static void doTheCropsOfHalfTheField(){
        //Magic numbers
        for(int i=0;i<5;i++){
            System.out.println("Processing img "+i);
            String name=names[i];           
            for(int ch=0;ch<5;ch++){
                ImagePlus img=IJ.openImage(mainDir+"/RawHighRes/channel_registered/"+name+"_channel_"+ch+".tif");
                System.out.println(mainDir+"/RawHighRes/channel_registered/"+name+"_channel_"+ch+".tif");
                img=VitimageUtils.cropImage(img, x0, y0, 0, x1-x0+1, y1-y0+1, 1);
                IJ.saveAsTiff(img, mainDir+"/SemiRaster/Raw/"+name+"_channel_"+ch+".tif");
            }
        }
    }


    public static void generateRGB(){
        for(int i=0;i<5;i++){
            String name=names[i];
            ImagePlus img0=IJ.openImage(mainDir+"/SemiRaster/Raw/"+name+"_channel_0.tif");
            ImagePlus img1=IJ.openImage(mainDir+"/SemiRaster/Raw/"+name+"_channel_1.tif");
            ImagePlus img2=IJ.openImage(mainDir+"/SemiRaster/Raw/"+name+"_channel_2.tif");
            img0.show();
            img1.show();
            img2.show();
            IJ.run(img0, "Merge Channels...", "c1="+name+"_channel_2.tif c2="+name+"_channel_1.tif c3="+name+"_channel_0.tif");
            ImagePlus img=IJ.getImage();
            IJ.saveAsTiff(img, mainDir+"/SemiRaster/RGB/"+name+".tif");

        }
    }
            
            
    public static void annotateSemiRaster(){
        //This step is done manually, by using the pointroi tool to generate 5 roi files
        return;
    }



    public static void registerWithLandmarksSemiRaster(){
        //Read the files /home/rfernandez/Bureau/A_Test/Test_Sergio/Data_4/SemiRaster/Rois/roi_i.roi and use it to generate a tab of correspondance points and a dense field

        
        Point3d[][]tab=new Point3d[5][];
        ImagePlus []tabImg=new ImagePlus[5];
        for(int i=0;i<5;i++){
            System.out.println("Processing roi "+i);
            tabImg[i]=IJ.openImage(mainDir+"/SemiRaster/RGB/"+names[i]+".tif");            
            String pathToRoi=mainDir+"/SemiRaster/Rois/Roi_"+(i+1)+".roi";
            //tabImg[i].show();
            RoiManager rm=RoiManager.getRoiManager();
            rm.reset();
            rm.runCommand("open", pathToRoi);
//            getRoi
            //with the pointroi, get the number of points
            int N=rm.getRoi(0).getContainedPoints().length;
            Point[]points=rm.getRoi(0).getContainedPoints();

            tab[i]=new Point3d[N];
            System.out.println(N+"");
            for(int indP=0;indP<N;indP++){
                tab[i][indP]=new Point3d(points[indP].getX() , points[indP].getY() ,  0);
            }
        }
        double sigma=tabImg[0].getWidth()/ratioFactorForSigmaComputation;
        System.out.println("Ok rois "+tab[0].length);

        ItkTransform[]tabTr=new ItkTransform[4];
        ItkTransform[]tabTrGlob=new ItkTransform[4];
        for(int i=1;i<4;i++){
            System.out.println("Processing img "+i);
            tabTr[i]=new ItkTransform();
            System.out.println("toto1");
            tabTr[i]=ItkTransform.computeDenseFieldFromSparseCorrespondancePoints(new Point3d[][]{tab[0],tab[i+1]}, tabImg[0], sigma);
            System.out.println("toto2");
            ImagePlus imgTrans=tabTr[i].transformImage(tabImg[i], tabImg[i+1]);
            System.out.println("toto3");
            IJ.saveAsTiff(imgTrans, mainDir+"/SemiRaster/Registered/Glob/"+names[i+1]+".glob.tif");
            System.out.println("toto4");
            tabTr[i].writeAsDenseField(mainDir+"/SemiRaster/Transformations/"+names[i+1]+".dense.tif", tabImg[0]);        
            System.out.println("toto5");
        }
    }
       




    public static void automaticRegistrationOfSemiRaster(){
        return;
    }

}
