package datastructure;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import javax.media.j3d.BadTransformException;
import javax.media.j3d.BranchGroup;
import javax.media.j3d.Transform3D;
import javax.media.j3d.TransformGroup;
import javax.vecmath.Point3f;
import javax.vecmath.Vector3d;
import javax.vecmath.Vector3f;
import CSM.CSMHeader;
import CSM.CSMPoints;
import Java3D.SkeletMaker.SkeletConnections;
import Misc.StaticTools;
import com.sun.j3d.utils.geometry.Cylinder;
import com.sun.j3d.utils.geometry.Sphere;

/*
 * Diese Klasse repraesentiert die Datenstruktur Skelett,
 * sie verwaltet die assoziationen von Messpunkten in gruppen von 
 * Koerperteilen und Verbindungspunkten bzw. Gelenken
 */
public class Skelett implements Serializable {
	private static final long serialVersionUID = -3347881594569855043L;
	BranchGroup skeletRoot;
	BranchGroup pointsGroup;
	BranchGroup bones;
	public Sphere[] points;
	TransformGroup[] pointTransforms;
	List<TransformGroup> boneTransformsAngle;
	List<TransformGroup> boneTransformsLenth;
	Point3f[] currentFrame = null;
	//SkeleteAppearance
	float scaleFactor = 0.01f;
	public float sphereSize = 0.5f;
	public float cylinderRadius = 0.2f;
	
	CSMHeader header;
	
//	public List<Integer> connectlist = new ArrayList<Integer>();
	public SkeletConnections connectlist = new SkeletConnections();
	public boolean[] printNodeName = new boolean[100];

	//public BranchGroup bg = new BranchGroup();
	
	public Skelett(CSMHeader header) {
		this.header = header;
		points = new Sphere[header.order.length]; // as many spheres as the header has markers
		pointTransforms = new TransformGroup[header.order.length];
		boneTransformsAngle = new ArrayList<TransformGroup>();
		boneTransformsLenth = new ArrayList<TransformGroup>();;
		
		//
		connectlist.setHeader(header);
		//
		init();
		setupDefaultTpose(header);
	}
	
	private void setupDefaultTpose(CSMHeader header) {
	
		Point3f[] tpose = CSMPoints.defaultTPose().points;
		
		for (int i = 0; i < header.order.length; i++) {
			//points[i] = new Sphere(0.5f);
			//points[i].setAppearance(Simple.green());
			Transform3D transform =  new Transform3D();
			tpose[i].scale(0.01f);
			transform.setTranslation(new Vector3f(tpose[i]));
			//System.out.println(tpose[i]);
			pointTransforms[i] = new TransformGroup(transform);
			pointTransforms[i].setCapability(TransformGroup.ALLOW_TRANSFORM_WRITE);
//			pointTransforms[i].addChild(points[i]);
			pointTransforms[i].addChild(StaticTools.createSphereWithText(new Point3f(0,0,0), sphereSize, header.order[i]));
			pointTransforms[i].setUserData(header.order[i]);
			
			pointsGroup.addChild(pointTransforms[i]);
		}
	}
	
	/**
	 * Returns TransformGroup for Specific Marker
	 * @param markerName (header.order[i])
	 * @return
	 */
	
	TransformGroup getTGfromMarkerName(String markerName)
	{
		for (TransformGroup tg : pointTransforms) {
			Object userData = tg.getUserData(); 
			if(userData != null && userData.getClass() == String.class)
			{
				String s = (String) userData;
				if(s.compareToIgnoreCase(markerName) == 0 )
					return tg;
			}
		}
		// hopefully we never reach this point :)
		System.out.println("Skeleton: getTGfromMakerName: no Matchon TG found for : " + markerName);
		return null;
	}
	
	
	
	
	public void loadFrame(Point3f[] frame)
	{
		currentFrame = frame;
		if (frame.length != points.length || frame.length != header.order.length)
		{
			System.err.println("Skelett: Load Frame: points Mismatch!");
			return;
		}
	// Setting new Translation for Spheres (points)
		Point3f p ;
		for (int i = 0; i < points.length; i++) {
			p = (Point3f) frame[i].clone();
			float z = p.z;
			p.z = p.y;
			p.y = z ;
			p.x*=-1;
			
			p.scale(Config.skeletScale);
			Transform3D t = new  Transform3D();
			t.setTranslation(new Vector3f(p));
			pointTransforms[i].setTransform(t) ;
			p =null;
		}
		// Setting new Translation for Bones
		for (int i = 0; i < connectlist.size()/2; i++) {
			//preveinting that the loaded skeleton adresses points higher than existing in this header
			if(connectlist.get(i*2) >= frame.length ||
					connectlist.get(i*2+1) >= frame.length ||
					i >= boneTransformsAngle.size())
				continue;
			Point3f a = (Point3f) frame[connectlist.get(i*2)].clone();
			float z = a.z;
			a.z = a.y;
			a.y = z;
			a.x*=-1;
			Point3f b = (Point3f) frame[connectlist.get(i*2 + 1)].clone();
			 z = b.z;
			b.z = b.y;
			b.y = z;
			b.x*=-1;
			a.scale(Config.skeletScale);
			b.scale(Config.skeletScale);
			moveBone(a, b, boneTransformsLenth.get(i), boneTransformsAngle.get(i));
		}
		
	}

	public BranchGroup getBG()
	{
			return	skeletRoot;

	}
	void init()
	{
		skeletRoot = new BranchGroup();
		skeletRoot.setCapability(BranchGroup.ALLOW_DETACH);
		skeletRoot.setCapability(BranchGroup.ALLOW_CHILDREN_WRITE);
		skeletRoot.setCapability(BranchGroup.ALLOW_CHILDREN_EXTEND);
		
		pointsGroup = new BranchGroup();
		pointsGroup.setCapability(BranchGroup.ALLOW_DETACH);
		pointsGroup.setCapability(BranchGroup.ALLOW_CHILDREN_WRITE);
		pointsGroup.setCapability(BranchGroup.ALLOW_CHILDREN_EXTEND);
		skeletRoot.addChild(pointsGroup);
		
		bones = new BranchGroup();
		bones.setCapability(BranchGroup.ALLOW_DETACH);
		bones.setCapability(BranchGroup.ALLOW_CHILDREN_EXTEND);
		bones.setCapability(BranchGroup.ALLOW_CHILDREN_WRITE);	
		skeletRoot.addChild(bones);

		initConnectList();
		
		//initConnections();
	//	initFillBones();
	}
	
	private void moveBone(Point3f a, Point3f b,TransformGroup boneLength, TransformGroup boneAngle)
	{
		float length = a.distance(b);
		Transform3D stretchZ = new Transform3D();
		stretchZ.setScale(new Vector3d(1,length,1));
		boneLength.setTransform(stretchZ);
		Transform3D t3d = null;
		try {
			t3d = StaticTools.getTransform(a, b);
			boneAngle.setTransform(StaticTools.getTransform(a, b));
		} catch (BadTransformException bte) {
			System.out.println("Skelet: MoveBone: Bad Transform Exception : " + t3d);
		}
	}
	
	
	/* Initing Default Bone Connections
	*/
	void initConnectList()
	{
		//head
		connect("LFHD","LBHD"); 
		connect("LFHD","RFHD");
	
		connect("RBHD","LBHD");
		connect("RFHD","LBHD");
		connect("RFHD","RBHD");
		
		//right arm
		connect("RUPA","RSHO");
		connect("RUPA","RELB");
		connect("RELB","RFRM");
		connect("RFRM","RFIN");
		connect("RWRA","RWRB");
		
		//left arm
		connect("LUPA","LSHO");
		connect("LUPA","LELB");
		connect("LELB","LFRM");
		connect("LFRM","LFIN");
		connect("LWRA","LWRB");
				// right leg
		connect("RPel","RTHI");
		connect("RTHI","RKNE");
		
		connect("RKNE","RSHN");
		connect("RSHN","RANK");
		connect("RANK","RHEL");
		connect("RTOE","RMT5");
		connect("RANK","RHEE");
		connect("RMT5","RHEE");
		connect("RTOE","RHEE");
		
		//fuss right
		connect("RHEL","RTOE");
		connect("RHEL","RMT5");
		
		// left leg
		connect("LPel","LTHI");
		connect("LTHI","LKNE");
		
		connect("LKNE","LSHN");
		connect("LSHN","LANK");
		connect("LANK","LHEL");
		connect("LANK","LHEE");
		connect("LTOE","LMT5");
		connect("LMT5","LHEE");
		connect("LTOE","LHEE");
		
		//fuss left
		connect("LHEL","LTOE");
		connect("LHEL","LMT5");
	
		// sholders
		connect("RSHO","LSHO");
	
		// shoulder hump connection
		connect("LSHO","LPel");
		connect("RSHO","RPel");
		
		//  guertel
		connect("RBWT","LBWT");
		connect("RPel","RBWT");
		connect("RPel","RFWT");
		connect("LPel","LBWT");
		connect("LPel","LFWT");
		connect("RFWT","LFWT");
		
		// ruecken
		connect("LBWT","T10");
		connect("RBWT","T10");
		connect("C7","T10");
		connect("C7","RSHO");
		connect("C7","LSHO");
 
		// bauch
		connect("LFWT","STRN");
		connect("RFWT","STRN");
		connect("CLAV","STRN");
		connect("CLAV","RSHO");
		connect("CLAV","LSHO");
		
		initBoneGroups();
		
	}
	
	public void setConnections(SkeletConnections newList)
	{
		bones.removeAllChildren();
		boneTransformsAngle.clear();
		boneTransformsLenth.clear();
		connectlist = newList;
		initBoneGroups();
		if(currentFrame != null)
			loadFrame(currentFrame);
	}
	
	private void initBoneGroups() {
		for (int i = 0; i < connectlist.size()/2; i++) {
			TransformGroup leng = new TransformGroup();
			leng.setCapability(TransformGroup.ALLOW_TRANSFORM_WRITE);
			boneTransformsLenth.add(leng);
			TransformGroup rot= new TransformGroup();
			rot.setCapability(TransformGroup.ALLOW_TRANSFORM_WRITE);
			boneTransformsAngle.add(rot);
			Cylinder c = new Cylinder(cylinderRadius,1);
			c.setAppearance(StaticTools.createAppearance());
			rot.addChild(leng);
			leng.addChild(c);
			BranchGroup bg  = new BranchGroup();
			bg.setCapability(BranchGroup.ALLOW_DETACH);
			bg.addChild(rot);
			bones.addChild(bg);
		}
	}
	
	public CSMHeader getHeader()
	{
		return header;
	}

	void connect(String a,String b)
	   {
		   int i= header.getPos(a);
		   int j = header.getPos(b);
		   if (i< 0 || j < 0)
			   return ;
		   connectlist.add(a);
		   connectlist.add(b);
	   }
}
