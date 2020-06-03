package info.marcobrandizi.learn.lucene;

import static org.junit.Assert.assertEquals;

import java.util.HashMap;
import java.util.Map;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.core.KeywordAnalyzer;
import org.apache.lucene.analysis.miscellaneous.PerFieldAnalyzerWrapper;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.Term;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.BooleanClause.Occur;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.PhraseQuery;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.store.RAMDirectory;
import org.junit.ClassRule;
import org.junit.Test;

/**
 * This addresses the problem of mixed field types, requiring mixed analyzers,
 * Details <a href = "https://stackoverflow.com/questions/62119328">here</a>.
 *
 * @author brandizi
 * <dl><dt>Date:</dt><dd>3 Jun 2020</dd></dl>
 *
 */
public class IDLuceneTest
{
	
	@ClassRule
	public static LuceneHelper idxmgr = LuceneHelper.of ( lh -> 
	{
		// No pun intended!!!
		Map<String,Analyzer> anals = new HashMap<> ();
		anals.put ( "docId", new KeywordAnalyzer () );
		lh.analyzer = new PerFieldAnalyzerWrapper ( new StandardAnalyzer (), anals );

		lh.index = new RAMDirectory ();
		IndexWriterConfig cfg = new IndexWriterConfig ( lh.analyzer );
		try ( IndexWriter w = new IndexWriter ( lh.index, cfg ) )
		{
			lh.addDoc ( w, "Sample title 1", "smp1" );
			lh.addDoc ( w, "Sample title 2", "SMP1" );

			lh.addDoc ( w, "DUP Doc 1", "DUP1" );
			lh.addDoc ( w, "DUP Doc 2", "DUP1" );
			lh.addDoc ( w, "DUP Doc 2", "DUP2" );
			lh.addDoc ( w, "DUP Doc 3", "DUP2" );
		}
	});
	
	@Test
	public void testLowCaseIdSearch () throws Exception
	{
		ScoreDoc[] scoreDocs = idxmgr.logResults ( idxmgr.searchAllFields ( "smp1" ) );
		assertEquals ( "Wrong no of results for lower case", 1, scoreDocs.length );
	}

	@Test
	public void testUpCaseIdSearch () throws Exception
	{
		ScoreDoc[] scoreDocs = idxmgr.logResults ( idxmgr.searchAllFields ( "SMP1" ) );
		assertEquals ( "Wrong no of results for upper case", 1, scoreDocs.length );
	}

	
	@Test
	public void testMixedCaseIdSearch () throws Exception
	{
		ScoreDoc[] scoreDocs = idxmgr.logResults ( idxmgr.searchAllFields ( "Smp1" ) );
		assertEquals ( "Wrong no of results for case mismatch", 0, scoreDocs.length );
	}

	@Test
	public void testPhraseQuery () throws Exception
	{
		PhraseQuery pq = new PhraseQuery ( "docId", "SMP1" );
		ScoreDoc[] scoreDocs = idxmgr.logResults ( idxmgr.search ( pq ) );
		assertEquals ( "Wrong no of results for upper case + phrase query", 1, scoreDocs.length );
	}
	
	@Test
	public void testParser () throws Exception
	{
		BooleanQuery.Builder qb = new BooleanQuery.Builder ();
		QueryParser qp = new QueryParser ( "docId", idxmgr.analyzer );
		qb.add ( qp.createPhraseQuery ( "docId", "DUP1" ), Occur.MUST );
		qb.add ( qp.createPhraseQuery ( "title", "DUP Doc 2" ), Occur.MUST );
		
		ScoreDoc[] scoreDocs = idxmgr.logResults ( idxmgr.search ( qb.build () ) );
		assertEquals ( "Doc not found (phrase query + parser)", 1, scoreDocs.length );
	}
	
	@Test
	public void testDisjunctionAndParser () throws Exception
	{
		BooleanQuery.Builder qb = new BooleanQuery.Builder ();
		QueryParser qp = new QueryParser ( "docId", idxmgr.analyzer );
		qb.add ( qp.createPhraseQuery ( "docId", "DUP2" ), Occur.MUST );
		
		BooleanQuery.Builder orqb = new BooleanQuery.Builder ();
		orqb.add ( qp.createPhraseQuery ( "title", "DUP Doc 2" ), Occur.SHOULD );
		orqb.add ( qp.createPhraseQuery ( "title", "Dup doc 3" ), Occur.SHOULD );
		qb.add ( orqb.build (), Occur.MUST );
		
		ScoreDoc[] scoreDocs = idxmgr.logResults ( idxmgr.search ( qb.build () ) );
		assertEquals ( "Wrong no of results (disjunction + parser)", 2, scoreDocs.length );
	}
	
	@Test
	public void testDisjunctionAndParserAndTermQueries () throws Exception
	{
		BooleanQuery.Builder qb = new BooleanQuery.Builder ();
		qb.add ( new TermQuery ( new Term ( "docId", "DUP2" ) ), Occur.MUST );
		
		BooleanQuery.Builder orqb = new BooleanQuery.Builder ();
		orqb.add ( new TermQuery ( new Term ( "title", "DUP Doc 2" ) ), Occur.SHOULD );
		orqb.add ( new TermQuery ( new Term ( "title", "Dup doc 3" ) ), Occur.SHOULD );
		qb.add ( orqb.build (), Occur.MUST );
		
		ScoreDoc[] scoreDocs = idxmgr.logResults ( idxmgr.search ( qb.build () ) );
		assertEquals ( "Wrong no of results (disjunction + parser + term queries)", 0, scoreDocs.length );
	}
}
