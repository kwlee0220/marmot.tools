package marmot.geo.command;

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
import marmot.RecordSet;
import marmot.geo.geotools.SimpleFeatures;
import marmot.geo.query.GeoDataStore;

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
	
	private SimpleFeatureCollection read(DataSet ds) {
		return SimpleFeatures.toFeatureCollection(ds);
	}
}
