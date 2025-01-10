package io.github.rocsg.dady.utils;

import ij.IJ;
import ij.ImagePlus;
import ij.gui.PointRoi;
import ij.gui.Roi;
import ij.plugin.frame.RoiManager;
import io.github.rocsg.fijiyama.common.VitimageUtils;
import io.github.rocsg.fijiyama.registration.TransformUtils;
import math3d.Point3d;

public class RegistrationUtils {
    
	public static void main(String[] args) {
		testRun();
	}

	public static void testRun() {
		//Generate a tab with two point3d
		Point3d[][]tab=new Point3d[2][2];
		tab[0][0]=new Point3d(0,0,0);
		tab[1][0]=new Point3d(0,0,0);
		tab[0][1]=new Point3d(1,1,1);
		tab[1][1]=new Point3d(1,1,1);
		System.out.println("Stats = "+getStatsAboutCorrespondancePoints(tab));
	}
	public static void writePointRoiToCsv(String pathToRoi,String pathToCsv){
		Point3d[]tab=getCoordinatesFromRoiSet(pathToRoi);
		int N=tab.length;
		double[][]tabdou=new double[N][3];
		for (int i=0;i<N;i++){
			tabdou[i][0]=tab[i].x;
			tabdou[i][1]=tab[i].y;
			tabdou[i][2]=tab[i].z;
		}
		VitimageUtils.writeDoubleArrayInFile(tabdou, pathToCsv);
	}

	public static String getStatsAboutCorrespondancePoints(Point3d[][]tab){
		int N=tab[0].length;
		double[]norms=new double[N];
		for(int n=0;n<N;n++){
			norms[n]=tab[0][n].distanceTo(tab[1][n]);
			System.out.println(n+" = "+norms[n]);
		}


		double[]tab1=VitimageUtils.statistics1D(norms);
		double[]tab2=VitimageUtils.minAndMaxOfTab(norms);
		return ("Stats : [ min="+tab2[0]+" | max="+tab2[1]+" | mean="+tab1[0]+" | std="+tab1[1]+" ]");
	}

	public static void saveRoiAs(Roi []r,String path){
		RoiManager rm = RoiManager.getInstance();
        rm.reset();
		for(Roi rr:r)rm.addRoi(rr);
		rm.runCommand("Save", path);
	}

	public static Point3d[][] getCoordinatesFromRoiSetOfCorrespondences(String path,ImagePlus imgRef,ImagePlus imgMov, boolean realWorldCoordinates){
		RoiManager rm=RoiManager.getRoiManager();
		rm.reset();
		IJ.open(path);
		int N=rm.getCount()/2;
		Point3d[][]tab=new Point3d[2][N];
		for(int indP=0;indP<N;indP++){
			tab[0][indP]=new Point3d(rm.getRoi(indP*2 ).getXBase() , rm.getRoi(indP * 2).getYBase() ,  rm.getRoi(indP * 2).getZPosition());
			tab[1][indP]=new Point3d(rm.getRoi(indP*2 +1 ).getXBase() , rm.getRoi(indP * 2 +1 ).getYBase() ,  rm.getRoi(indP * 2 +1 ).getZPosition());
			if(realWorldCoordinates) {
				tab[0][indP]=TransformUtils.convertPointToRealSpace(tab[0][indP],imgRef);
				tab[1][indP]=TransformUtils.convertPointToRealSpace(tab[1][indP],imgMov);
								
			}	
		}
		rm.reset();
		return tab;
	}


	public static Point3d[] getCoordinatesFromRoiSet(String path){
		RoiManager rm=RoiManager.getRoiManager();
		rm.reset();
		IJ.open(path);
		int N=rm.getCount();
		Point3d[]tab=new Point3d[N];
		for(int indP=0;indP<N;indP++){
			tab[indP]=new Point3d(rm.getRoi(indP ).getXBase() , rm.getRoi(indP).getYBase() ,  rm.getRoi(indP).getZPosition());
		}
		rm.reset();
		return tab;
	}
	
	public static Point3d[] getCoordinatesFromRoiSet(String path,ImagePlus img,boolean realWorldCoordinates){
		RoiManager rm=RoiManager.getRoiManager();
		rm.reset();
		IJ.open(path);
		int N=rm.getCount();
		Point3d[]tab=new Point3d[N];
		for(int indP=0;indP<N;indP++){
			tab[indP]=new Point3d(rm.getRoi(indP ).getXBase() , rm.getRoi(indP).getYBase() ,  rm.getRoi(indP).getZPosition());
			if(realWorldCoordinates) {
			tab[indP]=TransformUtils.convertPointToRealSpace(tab[indP],img);
			}
		}
		rm.reset();
		return tab;
	}
		public static PointRoi []getPointRoiTabMonoSliceFromPoint3dTab(Point3d[]tab,ImagePlus imgRef,boolean convertFromRealWorldCoordinates){
		int nPoints = tab.length;
		System.out.println("NN="+nPoints);
        float[] xPoints = new float[1];
        float[] yPoints = new float[1];
		PointRoi[]tabRoi=new PointRoi[nPoints];
		double vx=1;
		double vy=1;
        if(convertFromRealWorldCoordinates){
			vx=VitimageUtils.getVoxelSizes(imgRef)[0];
			vy=VitimageUtils.getVoxelSizes(imgRef)[1];
		} 
        for (int i = 0; i < nPoints; i++) {
            xPoints[0] = (float) (tab[i].x/vx);
            yPoints[0] = (float) (tab[i].y/vy);
			tabRoi[i]=new PointRoi(xPoints, yPoints, 1);
		}
		return tabRoi;
	}


	public static Point3d[]point3dDoubleTabToSingleTab(Point3d[][]tab2d){
		Point3d[]tab1d=new Point3d[tab2d[0].length*2];
		for(int i =0;i<tab2d[0].length;i++){
			tab1d[i*2]=tab2d[0][i];
			tab1d[i*2+1]=tab2d[1][i];
		}
		return tab1d;
	}



	
	public static void writePoint3dTabToCsv(Point3d[]tab,String pathToCsv){
		int N=tab.length;
		double[][]tabdou=new double[N][3];
		for (int i=0;i<N;i++){
			tabdou[i][0]=tab[i].x;
			tabdou[i][1]=tab[i].y;
			tabdou[i][2]=tab[i].z;
		}
		VitimageUtils.writeDoubleTabInCsvSimple(tabdou,pathToCsv);
	}

}
