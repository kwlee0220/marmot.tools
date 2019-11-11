package marmot.geo.command;

import java.util.List;

import org.geotools.data.DataUtilities;
import org.geotools.data.FeatureSource;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureSource;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.map.MapContext;
import org.geotools.map.StyleLayer;
import org.geotools.styling.SLD;
import org.geotools.styling.Style;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import com.vividsolutions.jts.geom.Envelope;

import marmot.DataSet;
import marmot.GeometryColumnInfo;
import marmot.MarmotRuntime;
import marmot.Plan;
import marmot.Record;
import marmot.RecordSet;
import marmot.geo.geotools.SimpleFeatures;
import marmot.geo.query.GeoDataStore;
import utils.stream.FStream;

/**
 * 
 * @author Kang-Woo Lee (ETRI)
 */
@SuppressWarnings("deprecation")
public class MarmotDataSetLayer extends StyleLayer {
	private final MapContext m_context;
	private final GeoDataStore m_store;
	private final DataSet m_ds;
	private final SimpleFeatureType m_sfType;
	private final CoordinateReferenceSystem m_crs;
	private final ReferencedEnvelope m_datasetBounds;
	
	private ReferencedEnvelope m_range;
	private FeatureSource<?,?> m_source;
	
	public static MarmotDataSetLayer create(MapContext context, GeoDataStore store, DataSet ds) {
		SimpleFeatureType sfType = SimpleFeatures.toSimpleFeatureType(ds.getId(), ds);
        Style style = SLD.createSimpleStyle(sfType);
        
        return new MarmotDataSetLayer(context, store, ds, sfType, style);
	}
	
	private MarmotDataSetLayer(MapContext context, GeoDataStore store, DataSet ds,
								SimpleFeatureType sfType, Style style) {
		super(style);
		
		m_context = context;
		m_store = store;
		m_ds = ds;
		m_sfType = sfType;
		m_crs = sfType.getCoordinateReferenceSystem();
		m_datasetBounds = new ReferencedEnvelope(ds.getBounds(), m_crs);
		
		m_range = m_datasetBounds;
		m_source = query(m_range);
	}

	@Override
	public ReferencedEnvelope getBounds() {
		return m_datasetBounds;
	}
	
	@Override
    public synchronized FeatureSource<?, ?> getFeatureSource() {
		ReferencedEnvelope range = m_context.getViewport().getBounds();
		if ( !range.equals(m_range) ) {
			m_source = query(m_range = range);
		}
		
		return m_source;
    }
	
	private SimpleFeatureSource query(Envelope bounds) {
		RecordSet rset = m_store.createRangeQuery(m_ds.getId(), bounds).run();
		 return DataUtilities.source(SimpleFeatures.toFeatureCollection(m_sfType, rset.toList()));
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
		return SimpleFeatures.toFeatureCollection(ds);
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
	
	private RecordSet sample2(DataSet ds, long count) {
		double ratio = (double)count / ds.getRecordCount();
		Plan plan = Plan.builder()
						.load(ds.getId())
						.sample(ratio)
						.build();
		MarmotRuntime marmot = ds.getMarmotRuntime();
		return marmot.executeToRecordSet(plan);
	}
	
	private List<Record> sample2(DataSet ds, Envelope bounds, long count) {
		Plan plan = Plan.builder()
						.query(ds.getId(), bounds)
						.build();
		MarmotRuntime marmot = ds.getMarmotRuntime();
		List<Record> selected = marmot.executeToRecordSet(plan).toList();
		double ratio = (double)count / selected.size();
		System.out.printf("%d / %d -> %.2f%n", count, selected.size(), ratio);
		
		return FStream.from(selected).sample(ratio).toList();
	}
}
