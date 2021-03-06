package CSM;

import java.io.Serializable;
import java.util.Locale;
import java.util.Scanner;

import javax.vecmath.Point3f;

/*
 * Klasse fuer eine Zeile von Punkten aus einer CSM datei;
 */
public class CSMPoints implements Serializable{
	private static final long serialVersionUID = 5965309329658371093L;
	/*
	 * Verlingung auf CSMHeader.order
	 */
	public Point3f[] points;
	int tos = 0;
	
	public CSMPoints(int length) {
		points = new Point3f[length];
	}
	public CSMPoints(Point3f[] points) {
		this.points = points;
		tos = points.length;
	}
	
	public void add(Point3f p )
	{
		if (tos < points.length)
		{
			points[tos++] = p ;
		}
		else 
		{
			System.out.println("Too many points");
		}
	}

	public String toString()
	{
		String values = new String();
		for (Point3f coord : points) {
			values += coord;
		}
		return "CSMPoints: Marker Count: " + points.length + values;
	}
	
	public static CSMPoints defaultTPose(){
		CSMPoints csm_p = new CSMPoints(CSMHeader.defaultHeader().order.length);
		String tpose = "29.005922 300.975311 1716.817627  -112.837563 293.209778 1716.918335  24.909645 457.027283 1710.989624  -109.101463 467.159729 1714.516235  -43.571762 489.973206 1507.755249  -38.192719 516.157593 1262.507446  -19.194544 301.917053 1450.501709  -38.609184 258.910217 1302.808228  -135.334290 516.787231 1441.976196  78.404083 412.206879 1550.348267  266.959351 430.072784 1533.915527  397.652954 416.843811 1555.218506  530.955139 421.221161 1540.150269  645.222900 312.372009 1517.818604  658.061890 446.482971 1528.575073  720.550720 357.201935 1529.898926  -161.758453 431.308624 1556.735474  -363.061707 418.387207 1538.878418  -496.220886 415.422424 1547.834473  -622.015137 393.737366 1538.022705  -721.137451 323.683411 1530.994019  -728.496277 454.064423 1508.870239  -810.691956 367.194946 1543.184204  66.542755 280.246582 971.347595  -122.579437 268.615387 968.231140  142.576904 380.397095 999.566284  -229.813507 365.540161 986.393982  57.619354 510.099396 999.505615  -150.310486 497.680664 1011.352356  189.710739 394.052551 780.104736  199.334106 415.855438 559.298340  229.657913 455.472931 374.824310  232.457382 454.089844 185.393326  157.401077 502.233978 39.053848  277.951019 251.860275 56.412464  324.975922 305.377441 48.372616  -271.966156 353.327698 767.455261  -277.525696 392.602844 547.434570  -311.397675 446.262207 358.335938  -307.833099 471.523621 179.019638  -251.646210 532.298889 46.597149  -345.551880 267.736298 53.943066  -395.018555 311.713562 49.646618  ";
		Scanner s = new Scanner(tpose);
		s.useLocale(Locale.US);
		for (int i = 0; i < CSMHeader.defaultHeader().order.length ; i++) {
			Point3f p = new Point3f(s.nextFloat(),s.nextFloat(),s.nextFloat());
			csm_p.add(p);
		}
		return csm_p;
	}
}
