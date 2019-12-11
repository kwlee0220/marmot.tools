package marmot.geo.command;

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.map.DefaultMapContext;
import org.geotools.map.MapContext;
import org.geotools.swt.SwtMapFrame;

import com.google.common.io.Files;

import marmot.DataSet;
import marmot.MarmotRuntime;
import marmot.command.MarmotClientCommand;
import marmot.command.MarmotClientCommands;
import marmot.geo.geotools.SimpleFeatures;
import marmot.geo.query.GeoDataStore;
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
@Command(name="mc_view_dataset",
		parameterListHeading = "Parameters:%n",
		optionListHeading = "Options:%n",
		description="swing-based dataset viewer")
@SuppressWarnings("deprecation")
public class RemoteViewDataSetMain extends MarmotClientCommand {
	@Parameters(paramLabel="id", index="0", arity="1..*", description={"dataset id"})
	private List<String> m_dsIdList;

	@Option(names={"-sample"}, paramLabel="count", description={"sample count"})
	private int m_sampleCount = -1;

	@Option(names={"-prefetch"}, description={"use prefetch"})
	private boolean m_prefetch = false;
	
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

		File parentDir = Files.createTempDir().getParentFile();
		File cacheDir =  new File(parentDir, "marmot_geoserver_cache");
		GeoDataStore store = GeoDataStore.from(marmot, cacheDir)
											.setSampleCount(m_sampleCount)
											.setUsePrefetch(m_prefetch);
		
	    String srid = null;
	    SimpleFeatureCollection sfColl;
		for ( String dsId: m_dsIdList ) {
			DataSet ds = marmot.getDataSet(dsId);
	        
			MarmotDataSetLayer layer = MarmotDataSetLayer.create(context, store, ds);
		    context.addLayer(layer);
		}
		
	    // and show the map viewer
	    SwtMapFrame.showMap(context);
	}
	
	private SimpleFeatureCollection read(DataSet ds) {
		return SimpleFeatures.toFeatureCollection(ds);
	}
}
