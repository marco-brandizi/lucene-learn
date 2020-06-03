package info.marcobrandizi.learn.lucene;

import static org.junit.Assert.assertEquals;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.core.KeywordAnalyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.store.RAMDirectory;
import org.junit.ClassRule;
import org.junit.Test;

/**
 * TODO: comment me!
 *
 * @author brandizi
 * <dl><dt>Date:</dt><dd>31 Aug 2017</dd></dl>
 *
 */
public class BasicLuceneTest
{
	
	@ClassRule
	public static LuceneHelper idxmgr = LuceneHelper.of ( lh -> 
	{
		lh.index = new RAMDirectory ();
		Analyzer analyzer = new StandardAnalyzer ();
		IndexWriterConfig cfg = new IndexWriterConfig ( analyzer );
		try ( IndexWriter w = new IndexWriter ( lh.index, cfg ) )
		{
			lh.addDoc ( w, "Just a Test", "doc1" );
			lh.addDoc ( w, "Another Test", "doc2" );
			lh.addDoc ( w, "Yet Another test", "doc3" );	
			lh.addDoc ( w, "My Personal Doc", "doc4" );
			
			// Obviously this is another doc, having the same ID (see last testIdMultipleMatches())
			lh.addDoc ( w, "Alternative Title", "doc4" );	
	
			lh.addDoc ( w, "Exact Search Sample 1", "doc10" );	
			lh.addDoc ( w, "Exact Search Sample 2", "doc10 0" );
			
			lh.addDoc ( w, "Mixed Case ID", "CamelDoc01" );	
		}		
	});
		
	@Test
	public void testTermSearch () throws Exception
	{
		ScoreDoc[] scoreDocs = idxmgr.logResults ( idxmgr.searchByTitle ( "test" ) );
		assertEquals ( "Wrong no. of results", 3, scoreDocs.length );
	}
		
	@Test
	public void testTermSearch2 () throws Exception
	{
		ScoreDoc[] scoreDocs = idxmgr.logResults ( idxmgr.searchByTitle ( "personal" ) );
		assertEquals ( "Wrong no. of results", 1, scoreDocs.length );
	}

	@Test
	public void testTermSearch3 () throws Exception
	{
		ScoreDoc[] scoreDocs = idxmgr.logResults ( idxmgr.searchAllFields ( "title:\"just a test\" OR docId:\"DOC2\"" ) );
		assertEquals ( "Wrong no. of results", 2, scoreDocs.length );
	}
	
	@Test
	public void testExactSearch () throws Exception
	{
		ScoreDoc[] scoreDocs = idxmgr.logResults ( idxmgr.searchAllFields ( "docId:'doc10'" ) );
		assertEquals ( "Wrong no. of results", 1, scoreDocs.length );
	}
		
	@Test
	public void testIdMultipleMatches () throws Exception
	{
		ScoreDoc[] scoreDocs = idxmgr.logResults ( idxmgr.searchAllFields ( "docId:\"doc4\"" ) );
		
		// Two Lucene docs, pointing to the same ID.
		assertEquals ( "Wrong no. of results", 2, scoreDocs.length );
	}
	
	/**
	 * @see https://stackoverflow.com/questions/62119328
	 */
	@Test
	public void testIdMixedCaseID () throws Exception
	{
		ScoreDoc[] scoreDocs = idxmgr.logResults ( idxmgr.searchAllFields ( "CamelDoc01" ) );
		assertEquals ( "This search should fail", 0, scoreDocs.length );
	}
	
	/**
	 * @see <a href = "https://stackoverflow.com/questions/62119328">here</a>.
	 */
	@Test
	public void testIdMixedCaseIDKeywordAnalyzer () throws Exception
	{
		idxmgr.analyzer = new KeywordAnalyzer ();
		ScoreDoc[] scoreDocs = idxmgr.logResults ( idxmgr.searchAllFields ( "CamelDoc01" ) );
		assertEquals ( "This should match", 1, scoreDocs.length );
	}
	
}
