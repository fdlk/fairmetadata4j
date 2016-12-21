/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package nl.dtl.fairmetadata.io;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import javax.annotation.Nonnull;
import nl.dtl.fairmetadata.model.DatasetMetadata;
import nl.dtl.fairmetadata.utils.vocabulary.DCAT;
import org.apache.logging.log4j.LogManager;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.vocabulary.DCTERMS;
import org.eclipse.rdf4j.model.vocabulary.XMLSchema;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.RDFParseException;
import org.eclipse.rdf4j.rio.Rio;
import org.eclipse.rdf4j.rio.UnsupportedRDFormatException;

/**
 * Parser for dataset metadata
 * 
 * @author Rajaram Kaliyaperumal
 * @since 2016-09-07
 * @version 0.1
 */
public class DatasetMetadataParser extends MetadataParser<DatasetMetadata> {
    
    private static final org.apache.logging.log4j.Logger LOGGER
            = LogManager.getLogger(DatasetMetadataParser.class);
    
    @Override
    protected DatasetMetadata createMetadata() {
        return new DatasetMetadata();
    }
    
    /**
     * Parse RDF statements to dataset metadata object
     * 
     * @param statements    List of RDF statement list 
     * @param datasetURI    Dataset URI
     * @return              DatasetMetadata object 
     */
    @Override
    public DatasetMetadata parse(@Nonnull List<Statement> statements, 
            @Nonnull IRI datasetURI) {
        Preconditions.checkNotNull(datasetURI, 
                "Dataset URI must not be null.");
        Preconditions.checkNotNull(statements, 
                "Dataset statements must not be null.");
        LOGGER.info("Parsing dataset metadata");
        DatasetMetadata metadata = super.parse(statements, datasetURI);
        List<IRI> distributions = new ArrayList();
        ValueFactory f = SimpleValueFactory.getInstance();
        for (Statement st : statements) {
            Resource subject = st.getSubject();
            IRI predicate = st.getPredicate();
            Value object = st.getObject();
            
            if(subject.equals(datasetURI)) {
                if (predicate.equals(DCAT.LANDING_PAGE)) {
                    metadata.setLandingPage((IRI) object);
                } else if (predicate.equals(DCAT.THEME)) {
                    metadata.getThemes().add((IRI) object);
                } else if (predicate.equals(DCAT.CONTACT_POINT)) {
                    metadata.setContactPoint((IRI) object);
                } else if (predicate.equals(DCAT.KEYWORD)) {
                    metadata.getKeywords().add( f.createLiteral(object.
                            stringValue(), XMLSchema.STRING));
                } else if (predicate.equals(DCAT.DISTRIBUTION)) {
                    distributions.add((IRI) object);
                } else if (predicate.equals(DCTERMS.ISSUED)) {
                    metadata.setDatasetIssued(f.createLiteral(object.
                            stringValue(), XMLSchema.DATETIME));
                } else if (predicate.equals(DCTERMS.MODIFIED)) {
                    metadata.setDatasetModified(f.createLiteral(object.
                            stringValue(), XMLSchema.DATETIME));
                }      
            }        
        }
        
        if(!distributions.isEmpty()) {
            metadata.setDistribution(distributions);
        }
        
        return metadata;
    }
    
    /**
     * Parse RDF string to dataset metadata object
     * @param datasetMetadata   Dataset metadata as a RDF string
     * @param datasetID         Dataset ID  
     * @param datasetURI        Dataset URI
     * @param catalogURI        Catalog URI
     * @param format            RDF string's RDF format
     * @return                  DatasetMetadata object
     * @throws MetadataParserException 
     */
    public DatasetMetadata parse (@Nonnull String datasetMetadata, 
            @Nonnull String datasetID, @Nonnull IRI datasetURI, IRI catalogURI, 
            @Nonnull RDFFormat format) 
            throws MetadataParserException {
        Preconditions.checkNotNull(datasetMetadata, 
                "Dataset metadata string must not be null."); 
        Preconditions.checkNotNull(datasetID, "Dataset ID must not be null.");
        Preconditions.checkNotNull(datasetURI, "Dataset URI must not be null.");
        Preconditions.checkNotNull(format, "RDF format must not be null.");
        
        Preconditions.checkArgument(!datasetMetadata.isEmpty(), 
                "The dataset metadata content can't be EMPTY");
        Preconditions.checkArgument(!datasetID.isEmpty(), 
                "The dataset id content can't be EMPTY");        
        try {
            Model modelDataset = Rio.parse(new StringReader(datasetMetadata), 
                    datasetURI.stringValue(), format);
            Iterator<Statement> it = modelDataset.iterator();
            List<Statement> statements = ImmutableList.copyOf(it);
            
            DatasetMetadata metadata = this.parse(statements, datasetURI);
//            metadata.setIdentifier(new LiteralImpl(datasetID, 
//                    XMLSchema.STRING));
            metadata.setParentURI(catalogURI);             
            return metadata;
        } catch (IOException ex) {
            String errMsg = "Error reading dataset metadata content"
                    + ex.getMessage();
            LOGGER.error(errMsg);
            throw (new MetadataParserException(errMsg));
        } catch (RDFParseException ex) {
            String errMsg = "Error parsing dataset metadata content. "
                    + ex.getMessage();
            LOGGER.error(errMsg);
            throw (new MetadataParserException(errMsg));
        } catch (UnsupportedRDFormatException ex) {
            String errMsg = "Unsuppoerted RDF format. " + ex.getMessage();
            LOGGER.error(errMsg);
            throw (new MetadataParserException(errMsg));
        }
    }
    
    /**
     * Parse RDF string to dataset metadata object
     *
     * @param datasetMetadata Catalog metadata as a RDF string
     * @param baseURI
     * @param format RDF string's RDF format
     * @return DatasetMetadata object
     * @throws MetadataParserException
     */
    public DatasetMetadata parse(@Nonnull String datasetMetadata,
            IRI baseURI, @Nonnull RDFFormat format)
            throws MetadataParserException {
        Preconditions.checkNotNull(datasetMetadata,
                "Dataset metadata string must not be null.");
        Preconditions.checkNotNull(format, "RDF format must not be null.");

        Preconditions.checkArgument(!datasetMetadata.isEmpty(),
                "The dataset metadata content can't be EMPTY");
        try {
            Model modelCatalog;
            if (baseURI != null) {
                modelCatalog = Rio.parse(new StringReader(datasetMetadata),
                        baseURI.stringValue(), format);
            } else {
                String dummyURI = "http://example.com/dummyResource";
                modelCatalog = Rio.parse(new StringReader(datasetMetadata), 
                        dummyURI, format);
            }
            Iterator<Statement> it = modelCatalog.iterator();
            List<Statement> statements = ImmutableList.copyOf(it);
            IRI catalogURI = (IRI) statements.get(0).getSubject();
            DatasetMetadata metadata = this.parse(statements, catalogURI);
            metadata.setUri(null);
            return metadata;
        } catch (IOException ex) {
            String errMsg = "Error reading dataset metadata content"
                    + ex.getMessage();
            LOGGER.error(errMsg);
            throw (new MetadataParserException(errMsg));
        } catch (RDFParseException ex) {
            String errMsg = "Error parsing dataset metadata content. "
                    + ex.getMessage();
            LOGGER.error(errMsg);
            throw (new MetadataParserException(errMsg));
        } catch (UnsupportedRDFormatException ex) {
            String errMsg = "Unsuppoerted RDF format. " + ex.getMessage();
            LOGGER.error(errMsg);
            throw (new MetadataParserException(errMsg));
        }
    }
    
}
