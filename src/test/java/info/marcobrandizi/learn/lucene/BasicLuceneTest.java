package info.marcobrandizi.learn.lucene;

import java.io.IOException;
import java.security.Provider;
import java.util.function.Supplier;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field.Store;
import org.apache.lucene.document.StoredField;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.queryparser.classic.MultiFieldQueryParser;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.RAMDirectory;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.lucene.analysis.core.KeywordAnalyzer;

/**
 * TODO: comment me!
 *
 * @author brandizi
 * <dl><dt>Date:</dt><dd>31 Aug 2017</dd></dl>
 *
 */
public class BasicLuceneTest
{
	private Logger log = LoggerFactory.getLogger ( this.getClass () );
	
	private static Directory index = new RAMDirectory ();
	private Analyzer analyzer = new StandardAnalyzer ();
	private IndexReader idxRdr;
	private IndexSearcher searcher;	

	
	@BeforeClass
	public static void createIndex () throws Exception
	{
		index = new RAMDirectory ();
		Analyzer analyzer = new StandardAnalyzer ();
		IndexWriterConfig cfg = new IndexWriterConfig ( analyzer );
		try ( IndexWriter w = new IndexWriter ( index, cfg ) )
		{
			addDoc ( w, "Just a Test", "doc1" );
			addDoc ( w, "Another Test", "doc2" );
			addDoc ( w, "Yet Another test", "doc3" );	
			addDoc ( w, "My Personal Doc", "doc4" );
			
			// Obviously this is another doc, having the same ID (see last testIdMultipleMatches())
			addDoc ( w, "Alternative Title", "doc4" );	
	
			addDoc ( w, "Exact Search Sample 1", "doc10" );	
			addDoc ( w, "Exact Search Sample 2", "doc10 0" );
			
			addDoc ( w, "Mixed Case ID", "CamelDoc01" );	
		}
	}
	
	@Before
	public void initIndexReaders () throws Exception
	{
		idxRdr = DirectoryReader.open ( index );
		searcher = new IndexSearcher ( idxRdr );			
	}
	
	
	@Test
	public void testTermSearch () throws Exception
	{
		log.info ( "Search 1" );
		ScoreDoc[] scoreDocs = logResults ( searchByTitle ( "test" ) );
		Assert.assertEquals ( "Wrong no. of results", 3, scoreDocs.length );
	}
		
	@Test
	public void testTermSearch2 () throws Exception
	{
		log.info ( "Search 2" );
		ScoreDoc[] scoreDocs = logResults ( searchByTitle ( "personal" ) );
		Assert.assertEquals ( "Wrong no. of results", 1, scoreDocs.length );
	}

	@Test
	public void testTermSearch3 () throws Exception
	{
		log.info ( "Search 3" );
		ScoreDoc[] scoreDocs = logResults ( searchAllFields ( "title:\"just a test\" OR docId:\"DOC2\"" ) );
		Assert.assertEquals ( "Wrong no. of results", 2, scoreDocs.length );
	}
	
	@Test
	public void testExactSearch () throws Exception
	{
		log.info ( "Exact Search" );
		ScoreDoc[] scoreDocs = logResults ( searchAllFields ( "docId:'doc10'" ) );
		Assert.assertEquals ( "Wrong no. of results", 1, scoreDocs.length );
	}
		
	@Test
	public void testIdMultipleMatches () throws Exception
	{
		log.info ( "Search same doc with multiple titles" );
		ScoreDoc[] scoreDocs = logResults ( searchAllFields ( "docId:\"doc4\"" ) );
		
		// Two Lucene docs, pointing to the same ID.
		Assert.assertEquals ( "Wrong no. of results", 2, scoreDocs.length );
	}
	
	/**
	 * @see https://stackoverflow.com/questions/62119328
	 */
	@Test
	public void testIdMixedCaseID () throws Exception
	{
		log.info ( "Search mixed case ID" );
		ScoreDoc[] scoreDocs = logResults ( searchAllFields ( "CamelDoc01" ) );
		Assert.assertEquals ( "This search should fail", 0, scoreDocs.length );
	}
	
	/**
	 * @see https://stackoverflow.com/questions/62119328
	 */
	@Test
	public void testIdMixedCaseIDKeywordAnalyzer () throws Exception
	{
		log.info ( "Search mixed case ID, KeywordAnalyzer" );
		analyzer = new KeywordAnalyzer ();
		ScoreDoc[] scoreDocs = logResults ( searchAllFields ( "CamelDoc01" ) );
		Assert.assertEquals ( "This should match", 1, scoreDocs.length );
	}
	

	private static void addDoc ( IndexWriter w, String keyword, String id ) throws IOException
	{
		Document doc = new Document ();
		doc.add ( new TextField ( "title", keyword, Store.YES ) );
		doc.add ( new StringField ( "docId", id, Store.YES ) );
		doc.add ( new StoredField ( "note", "An example of non-indexed field" ) );
		w.addDocument ( doc );
	}
	
	private ScoreDoc[] search ( QueryParser queryParser, String queryStr ) 
		throws ParseException, IOException
	{
		Query q = queryParser.parse ( queryStr );
		TopDocs topDocs = searcher.search ( q, 10 );
		return topDocs.scoreDocs;
	}

	private ScoreDoc[] searchByTitle ( String queryStr ) 
		throws ParseException, IOException
	{
		return search ( new QueryParser ( "title", analyzer ), queryStr );
	}
		
	private ScoreDoc[] searchAllFields ( String queryStr ) 
		throws ParseException, IOException
	{
		return search ( new MultiFieldQueryParser ( new String [] { "title", "docId" }, analyzer ), queryStr );
	}
		
	private ScoreDoc[] logResults ( ScoreDoc[] searchResults ) throws IOException
	{
		for ( ScoreDoc scoreDoc: searchResults )
		{
			Document doc = searcher.doc ( scoreDoc.doc );
			log.info ( "Document found: '{}', '{}', score: {}", doc.get ( "title" ), doc.get ( "docId" ), scoreDoc.score );
		}
		return searchResults;
	}
}
