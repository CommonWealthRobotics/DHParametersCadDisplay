import com.neuronrobotics.sdk.addons.kinematics.IVitaminHolder

//Your code here
import com.neuronrobotics.bowlerstudio.creature.ICadGenerator;
import com.neuronrobotics.bowlerstudio.creature.MobileBaseCadManager

import org.apache.commons.io.IOUtils;
import com.neuronrobotics.bowlerstudio.vitamins.*;
import com.neuronrobotics.sdk.addons.kinematics.AbstractLink
import com.neuronrobotics.sdk.addons.kinematics.DHLink
import com.neuronrobotics.sdk.addons.kinematics.DHParameterKinematics
import com.neuronrobotics.sdk.addons.kinematics.LinkConfiguration
import com.neuronrobotics.sdk.addons.kinematics.MobileBase
import com.neuronrobotics.sdk.addons.kinematics.math.TransformNR
import java.nio.file.Paths;

import eu.mihosoft.vrl.v3d.CSG
import eu.mihosoft.vrl.v3d.Cube
import eu.mihosoft.vrl.v3d.Extrude
import eu.mihosoft.vrl.v3d.FileUtil;
import eu.mihosoft.vrl.v3d.Sphere
import eu.mihosoft.vrl.v3d.Transform;
import eu.mihosoft.vrl.v3d.parametrics.LengthParameter
import javafx.scene.transform.Affine;
import com.neuronrobotics.bowlerstudio.physics.TransformFactory;
return new ICadGenerator(){
	private CSG moveDHValues(CSG incoming,DHLink dh ){
		TransformNR step = new TransformNR(dh.DhStep(0)).inverse()
		Transform move = TransformFactory.nrToCSG(step)
		return incoming.transformed(move)

	}	
	ArrayList<CSG> linkLimitParts(DHParameterKinematics d, int linkIndex){
		ArrayList<DHLink> dhLinks = d.getChain().getLinks()
		if(linkIndex >= dhLinks.size()){
			return []
		}
		ArrayList<CSG> allCad=new ArrayList<>();
		DHLink dh = dhLinks.get(linkIndex)
		// Hardware to engineering units configuration
		LinkConfiguration conf = d.getLinkConfiguration(linkIndex);
		// Engineering units to kinematics link (limits and hardware type abstraction)
		AbstractLink abstractLink = d.getAbstractLink(linkIndex);// Transform used by the UI to render the location of the object
		// Transform used by the UI to render the location of the object
		def manipulator = dh.getListener();
		def lastLinkFrame

		CSG profile = new Cube(1,// x dimention
					20,// y dimention
				
				1//  Z dimention
				)
				.toCSG()// converts it to a CSG tor display
				.toYMin()
				.toZMin()
				//.toXMin()
		CSG theta;
		double thetaval = Math.toDegrees(dh.getTheta())
		if(Math.abs(thetaval)>10){
			theta= CSG.unionAll(
			Extrude.revolve(profile,
						0, // rotation center radius, if 0 it is a circle, larger is a donut. Note it can be negative too
						Math.abs(thetaval),// degrees through wich it should sweep
						(int)10)//number of sweep increments
			)
		}else{
			theta = profile
		}
		
		if(thetaval>0){
			theta= theta.rotz(-thetaval)
		}
		theta= theta.rotz(90)
		.movez(0.5)
		theta.setColor(javafx.scene.paint.Color.AQUA)


		def dpart = new Cube(1,1,dh.getD()>0?dh.getD():1).toCSG()
					.toZMin()
		double upperLimit = abstractLink.getMaxEngineeringUnits()
		double lowerLimit = -abstractLink.getMinEngineeringUnits()
		//double totalRange = -(upperLimit-lowerLimit)
		def min = 10
		if(upperLimit>360)
			upperLimit=360
		if(upperLimit<min)
			upperLimit=min
		def name = d.getScriptingName()
		//println name
		
		def printit = name.equals("FrontLeft")&&linkIndex==1
		//if(printit)println "\n\n\nLink range = "+totalRange+" "+upperLimit+" " +lowerLimit
		def orentationAdjust = -thetaval+90
//		CSG Rangeupper
//		//println "Range total " + rangeComp
//		if(upperLimit>10)
//			Rangeupper = CSG.unionAll(
//			Extrude.revolve(profile,
//					0, // rotation center radius, if 0 it is a circle, larger is a donut. Note it can be negative too
//					upperLimit,// degrees through wich it should sweep
//					(int)(upperLimit/12.0))//number of sweep increments
//			)
//		else
//			Rangeupper =profile
//		
//		Rangeupper=Rangeupper
//			.rotz(orentationAdjust-upperLimit)
//			.movez(-1.5)
//
//		
//		Rangeupper.setColor(javafx.scene.paint.Color.LIGHTGREEN)
		def upperLim = profile
					.rotz(-upperLimit+orentationAdjust)
					.movez(-2)
					.setColor(javafx.scene.paint.Color.HOTPINK)
		def lowerLim = profile
					.rotz(lowerLimit+orentationAdjust)
					.movez(-2)
					.setColor(javafx.scene.paint.Color.WHITE)
		def zeroLim = profile
					.rotz(orentationAdjust)
					.movez(-2)
					.setColor(javafx.scene.paint.Color.INDIGO)
//		CSG motor = Vitamins.get(conf.getElectroMechanicalType(),conf.getElectroMechanicalSize())
//			.roty(180)
//			.toZMin()
//			.rotz(90)
			
		def lastFrameParts = [
		theta,
		//motor,
		dpart,upperLim,lowerLim,zeroLim//,Rangeupper
		]
		
		LengthParameter tailLength		= new LengthParameter("Cable Cut Out Length",30,[500, 0.01])
		tailLength.setMM(10);

		for(CSG c:lastFrameParts) {
			c.setManufacturing({return null})
			c.getStorage().set("no-physics",true)
		}

		return lastFrameParts;
		
	}
	ArrayList<CSG> linkParts(DHParameterKinematics d, int linkIndex){
		ArrayList<CSG> allCad=new ArrayList<>();
		ArrayList<DHLink> dhLinks = d.getChain().getLinks()
		DHLink dh = dhLinks.get(linkIndex)
		double thetaval = Math.toDegrees(dh.getTheta())
		// Hardware to engineering units configuration
		LinkConfiguration conf = d.getLinkConfiguration(linkIndex);
		// Engineering units to kinematics link (limits and hardware type abstraction)
		AbstractLink abstractLink = d.getAbstractLink(linkIndex);// Transform used by the UI to render the location of the object
		// Transform used by the UI to render the location of the object
		def manipulator = dh.getListener();
		CSG shaft = moveDHValues(
			Vitamins.get(conf.getShaftType(),conf.getShaftSize())
			.rotz(90-thetaval)
			.toZMax()
			,dh)
		
		def massKg = Math.abs(conf.getMassKg())
		massKg=massKg>0?massKg:0.001
		def centerOfMass = TransformFactory.nrToCSG(conf.getCenterOfMassFromCentroid() )
		def CMvis = new Sphere(100*massKg).toCSG()
					.transformed(centerOfMass)
		
		if(linkIndex==0)
			lastLinkFrame=d.getRootListener()
		else
			lastLinkFrame=dhLinks.get(linkIndex-1).getListener();
		
		def rVal = new Cube(dh.getR()>0?dh.getR():5,1,1).toCSG()
					.toXMax()
					.toZMax()
		rVal.setColor(javafx.scene.paint.Color.RED)
		CSG profile = new Cube(1,// x dimention
					20,// y dimention
				
				1//  Z dimention
				)
				.toCSG()// converts it to a CSG tor display
				.toYMin()
				.toZMin()
				//.toXMin()
		CSG alpha;
		double alphaVal = Math.toDegrees(dh.getAlpha())
		if(Math.abs(alphaVal)>10){
			alpha= CSG.unionAll(
			Extrude.revolve(profile,
						0, // rotation center radius, if 0 it is a circle, larger is a donut. Note it can be negative too
						Math.abs(alphaVal),// degrees through wich it should sweep
						(int)10)//number of sweep increments
			)
			//.rotz(alphaVal<0?-alphaVal:0)
		}else{
			alpha = profile
		}
		alpha= alpha.roty(90)
		if(alphaVal>0){
			alpha= alpha.rotx(alphaVal)	
		}
		alpha= alpha
			.rotx(-90)
			.movex(-dh.getR())
		alpha.setColor(javafx.scene.paint.Color.YELLOW)

		//println name
		

		def parts = [rVal,alpha,CMvis
		//,shaft
		] as ArrayList<CSG>
		for(CSG c:parts) {
			c.setManufacturing({return null})
			c.getStorage().set("no-physics",true)
			
		}
		return parts;
		
	}
	@Override
	public ArrayList<CSG> generateCad(DHParameterKinematics d, int linkIndex) {
		ArrayList<DHLink> dhLinks = d.getChain().getLinks()
		DHLink dh = dhLinks.get(linkIndex)
		Affine manipulator = d.getListener(linkIndex);
		TransformNR offset = new TransformNR(dh.DhStep(0)).inverse();
		
		ArrayList<CSG> lastFrameParts = linkLimitParts( d,  linkIndex+1)
		MobileBaseCadManager manager  = MobileBaseCadManager.get(d.getLinkConfiguration(linkIndex));
		
//		if(linkIndex<(d.getNumberOfLinks()-1)) {
//			CSG vitamin = manager.getVitaminsElectroMechanicalDisplay(d.getAbstractLink(linkIndex+1),manipulator);
//			lastFrameParts.add(vitamin)
//		}
		
		ArrayList<CSG> parts = linkParts( d,  linkIndex)
		parts.addAll(lastFrameParts)
		for(int i=0;i<parts.size();i++){
			parts.get(i).setManipulator(manipulator);
			//parts.get(i).setColor(javafx.scene.paint.Color.RED)
		}
		Affine lastLinkAffine = linkIndex==0? d.getRootListener() :d.getListener(linkIndex-1);
		if(manager!=null) {
			parts.addAll(manager.getOriginVitaminsDisplay(
				d.getAbstractLink(linkIndex),
				manipulator,offset));
			parts.addAll(manager.getDefaultVitaminsDisplay(
				d.getAbstractLink(linkIndex),
				manipulator));
			parts.addAll(manager.getPreviousLinkVitaminsDisplay(
				d.getAbstractLink(linkIndex),
				lastLinkAffine));
		}else{
			println "No manager found for "+d.getScriptingName()+" "+linkIndex
		}
		for(CSG c:parts) {
			c.getStorage().set("no-physics",true)
		}
		if(manager!=null) {
			parts.addAll(manager.getOriginVitamins(
				d.getAbstractLink(linkIndex),
				manipulator,offset));
			parts.addAll(manager.getDefaultVitamins(
				d.getAbstractLink(linkIndex),
				manipulator));
			parts.addAll(manager.getPreviousLinkVitamins(
				d.getAbstractLink(linkIndex),
				lastLinkAffine));
		}
		for(CSG c:parts) {
			c.setManufacturing({return null})
		}
		return parts;

	}

	/**
	 * Gets the all dh chains.
	 *
	 * @return the all dh chains
	 */
	public ArrayList<DHParameterKinematics> getLimbDHChains(MobileBase base) {
		ArrayList<DHParameterKinematics> copy = new ArrayList<DHParameterKinematics>();
		for(DHParameterKinematics l:base.getLegs()){
			copy.add(l);	
		}
		for(DHParameterKinematics l:base.getAppendages() ){
			copy.add(l);	
		}
		return copy;
	}

@Override
	public ArrayList<CSG> generateBody(MobileBase b ) {
		ArrayList<CSG> allCad=new ArrayList<>();
		
		def massKg = b.getMassKg()
		def centerOfMass = TransformFactory.nrToCSG(b.getCenterOfMassFromCentroid() )
		def CMvis = new Sphere(100*massKg).toCSG()
					.transformed(centerOfMass)
					
		def centerOfIMU = TransformFactory.nrToCSG(b.getIMUFromCentroid() )
		def IMU = new Cube(5).toCSG()
					.transformed(centerOfIMU)
					.setColor(javafx.scene.paint.Color.WHITE)			
	// Load the .CSG from the disk and cache it in memory
		CSG body  = new Cube(4).toCSG();

		body.setManipulator(b.getRootListener());
		body.setColor(javafx.scene.paint.Color.GREY)
		def parts = [
		body,
		IMU, CMvis] as ArrayList<CSG>

		for(DHParameterKinematics l:getLimbDHChains(b)){
			TransformNR position = l.getRobotToFiducialTransform();
			Transform csgTrans = TransformFactory.nrToCSG(position)
			
			parts.addAll(linkLimitParts( l,  0).collect{
									it.transformed(csgTrans)
					}
				);			
		}

		for(int i=0;i<parts.size();i++){
			parts.get(i)
			.setManipulator(b.getRootListener());
		}
		MobileBaseCadManager manager  = MobileBaseCadManager.get(b);
		LengthParameter tailLength		= new LengthParameter("Cable Cut Out Length",30,[500, 0.01])
		tailLength.setMM(10);
		parts.addAll(manager.getVitaminsDisplay(b,b.getRootListener()));
		for(CSG c:parts) {
			c.getStorage().set("no-physics",true)
		}
		// these vitamins do not update live, but are computed for physics. 
		// they mark the location in cad the vitamin is after regeneration
		parts.addAll(manager.getVitamins(b,b.getRootListener()));
		for(CSG c:parts) {
			c.setManufacturing({return null})
		}
		return parts;
	}
}
