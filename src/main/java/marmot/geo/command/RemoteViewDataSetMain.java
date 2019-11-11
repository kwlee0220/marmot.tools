package marmot.geo.command;

import java.io.IOException;
import java.util.List;

import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.map.DefaultMapContext;
import org.geotools.map.MapContext;
import org.geotools.swt.SwtMapFrame;

import marmot.DataSet;
import marmot.GeometryColumnInfo;
import marmot.MarmotRuntime;
import marmot.Plan;
import marmot.command.MarmotClientCommand;
import marmot.command.MarmotClientCommands;
import marmot.geo.geotools.SimpleFeatures;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Help;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;


/**
 * </ol>
 * 
 * @author Kang-Woo Lee (ETRI)
 */
@Command(name="mv_view_dataset",
		parameterListHeading = "Parameters:%n",
		optionListHeading = "Options:%n",
		description="swing-based dataset viewer")
public class RemoteViewDataSetMain extends MarmotClientCommand {
	@Parameters(paramLabel="id", index="0", arity="1..*", description={"dataset id"})
	private List<String> m_dsIdList;

	@Option(names={"-sample"}, paramLabel="count", description={"sample count"})
	private int m_sampleCount = -1;
	
	public static final void main(String... args) throws Exception {
		MarmotClientCommands.configureLog4j();

		RemoteViewDataSetMain cmd = new RemoteViewDataSetMain();
		CommandLine.run(cmd, System.out, System.err, Help.Ansi.OFF, args);
	}
	
	@Override
	public void run() {
		try {
			run(getMarmotRuntime());
		}
		catch ( Exception e ) {
			System.err.printf("failed: %s%n%n", e);
		}
	}
	
	public final void run(MarmotRuntime marmot) throws IOException {
		MapContext context = new DefaultMapContext();
	    context.setTitle("Marmot DataSet Viewer");
		
	    String srid = null;
	    SimpleFeatureCollection sfColl;
		for ( String dsId: m_dsIdList ) {
			DataSet ds = marmot.getDataSet(dsId);
			GeometryColumnInfo gcInfo = ds.getGeometryColumnInfo();
			
			if ( srid == null ) {
				srid = gcInfo.srid();
			}
			if ( !srid.equals(gcInfo.srid()) ) {
				sfColl = (m_sampleCount > 0) ? sample(ds, gcInfo.srid(), m_sampleCount)
											: read(ds, gcInfo.srid());
			}
			else {
				sfColl = (m_sampleCount > 0) ? sample(ds, m_sampleCount) : read(ds);
			}
			
		    context.addLayer(sfColl, null);
		}
		
	    // and show the map viewer
	    SwtMapFrame.showMap(context);
	}
	
	private SimpleFeatureCollection sample(DataSet ds, String tarSrid, long count) {
		double ratio = (double)count / ds.getRecordCount();
		GeometryColumnInfo gcInfo = ds.getGeometryColumnInfo();

		Plan plan = Plan.builder()
						.load(ds.getId())
						.sample(ratio)
						.transformCrs(gcInfo.name(), gcInfo.srid(), tarSrid)
						.build();
		MarmotRuntime marmot = ds.getMarmotRuntime();
		return SimpleFeatures.toFeatureCollection(ds.getId(), marmot, plan,
												ds.getGeometryColumnInfo().srid());
	}

	private SimpleFeatureCollection read(DataSet ds, String tarSrid) {
		GeometryColumnInfo gcInfo = ds.getGeometryColumnInfo();

		Plan plan = Plan.builder()
						.load(ds.getId())
						.transformCrs(gcInfo.name(), gcInfo.srid(), tarSrid)
						.build();
		MarmotRuntime marmot = ds.getMarmotRuntime();
		return SimpleFeatures.toFeatureCollection(ds.getId(), marmot, plan,
												ds.getGeometryColumnInfo().srid());
	}
	
	private SimpleFeatureCollection read(DataSet ds) {
		return SimpleFeatures.toFeatureCollection(ds.getId(), ds);
	}
	
	private SimpleFeatureCollection sample(DataSet ds, long count) {
		double ratio = (double)count / ds.getRecordCount();
		Plan plan = Plan.builder()
						.load(ds.getId())
						.sample(ratio)
						.build();
		MarmotRuntime marmot = ds.getMarmotRuntime();
		return SimpleFeatures.toFeatureCollection(ds.getId(), marmot, plan,
												ds.getGeometryColumnInfo().srid());
	}
}
