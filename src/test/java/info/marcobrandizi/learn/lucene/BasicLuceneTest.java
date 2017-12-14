package info.marcobrandizi.learn.lucene;

import java.io.IOException;
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
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
	
	@Test
	public void testBasics () throws Exception
	{
		// Create
		Directory index = new RAMDirectory ();
		StandardAnalyzer analyzer = new StandardAnalyzer ();
		IndexWriterConfig cfg = new IndexWriterConfig ( analyzer );
		IndexWriter w = new IndexWriter ( index, cfg );

		addDoc ( w, "Just a Test", "doc1" );
		addDoc ( w, "Another Test", "doc2" );
		addDoc ( w, "Yet Another test", "doc3" );	
		addDoc ( w, "My Personal Doc", "doc4" );	
		// Obviously this is another doc, having the same ID (see last search below)
		addDoc ( w, "Alternative Title", "doc4" );	

		addDoc ( w, "Exact Search Sample 1", "doc10" );	
		addDoc ( w, "Exact Search Sample 2", "doc10 0" );	
		
		w.close ();
		
		
		// Search
		IndexReader idxRdr = DirectoryReader.open ( index );
		IndexSearcher searcher = new IndexSearcher ( idxRdr );	
		
		log.info ( "Search 1" );
		ScoreDoc[] scoreDocs = searchByTitle ( searcher, analyzer, "test" );
		logResults ( searcher, scoreDocs );
		Assert.assertEquals ( "Wrong no. of results", 3, scoreDocs.length );
		
		log.info ( "Search 2" );
		scoreDocs = searchByTitle ( searcher, analyzer, "personal" );
		logResults ( searcher, scoreDocs );
		Assert.assertEquals ( "Wrong no. of results", 1, scoreDocs.length );

		log.info ( "Search 3" );
		scoreDocs = searchAllFields ( searcher, analyzer, "title:\"just a test\" OR docId:\"DOC2\"" );
		logResults ( searcher, scoreDocs );
		Assert.assertEquals ( "Wrong no. of results", 2, scoreDocs.length );

		log.info ( "Exact Search" );
		scoreDocs = searchAllFields ( searcher, analyzer, "docId:'doc10'" );
		logResults ( searcher, scoreDocs );
		Assert.assertEquals ( "Wrong no. of results", 1, scoreDocs.length );
		
		log.info ( "Search same doc with multiple titles" );
		scoreDocs = searchAllFields ( searcher, analyzer, "docId:\"doc4\"" );
		logResults ( searcher, scoreDocs );
		// Two Lucene docs, pointing to the same ID.
		Assert.assertEquals ( "Wrong no. of results", 2, scoreDocs.length );
	}
	

	private void addDoc ( IndexWriter w, String keyword, String id ) throws IOException
	{
		Document doc = new Document ();
		doc.add ( new TextField ( "title", keyword, Store.YES ) );
		doc.add ( new StringField ( "docId", id, Store.YES ) );
		doc.add ( new StoredField ( "note", "An example of non-indexed field" ) );
		w.addDocument ( doc );
	}
	
	
	private ScoreDoc[] search ( IndexSearcher searcher, QueryParser queryParser, String queryStr ) 
		throws ParseException, IOException
	{
		Query q = queryParser.parse ( queryStr );
		TopDocs topDocs = searcher.search ( q, 10 );
		return topDocs.scoreDocs;
	}

	private ScoreDoc[] searchByTitle ( IndexSearcher searcher, final Analyzer analyzer, String queryStr ) 
		throws ParseException, IOException
	{
		return search ( searcher, new QueryParser ( "title", analyzer ), queryStr );
	}
	
	private ScoreDoc[] searchAllFields ( IndexSearcher searcher, Analyzer analyzer, String queryStr ) 
		throws ParseException, IOException
	{
		return search ( searcher, new MultiFieldQueryParser ( new String [] { "title", "docId" }, analyzer ), queryStr );
	}
	
	
	private void logResults ( IndexSearcher searcher, ScoreDoc[] scoreDocs ) throws IOException
	{
		for ( ScoreDoc scoreDoc: scoreDocs )
		{
			Document doc = searcher.doc ( scoreDoc.doc );
			log.info ( "Document found: '{}', '{}', score: {}", doc.get ( "title" ), doc.get ( "docId" ), scoreDoc.score );
		}
	}
}
