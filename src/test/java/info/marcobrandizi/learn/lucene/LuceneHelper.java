package info.marcobrandizi.learn.lucene;

import java.io.IOException;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field.Store;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.queryparser.classic.MultiFieldQueryParser;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.PhraseQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.RAMDirectory;
import org.junit.rules.ExternalResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.machinezoo.noexception.throwing.ThrowingConsumer;

/**
 * TODO: comment me!
 *
 * @author brandizi
 * <dl><dt>Date:</dt><dd>3 Jun 2020</dd></dl>
 *
 */
public class LuceneHelper extends ExternalResource
{
	public Directory index = new RAMDirectory ();
	public Analyzer analyzer = new StandardAnalyzer ();
	public IndexReader reader;
	public IndexSearcher searcher;
	
	protected Logger log = LoggerFactory.getLogger ( LuceneHelper.class );
	
	public static LuceneHelper of ( ThrowingConsumer<LuceneHelper> initialiser )
	{
		return new LuceneHelper ()
		{
			@Override
			protected void before () throws Throwable 
			{
				this.log.info ( "Index initialisation" );
				initialiser.accept ( this );
				this.reader = DirectoryReader.open ( index );
				this.searcher = new IndexSearcher ( reader );			
				this.log.info ( "Index initialised" );
			}
		};
	}
	
	public void addDoc ( IndexWriter w, String keyword, String id ) throws IOException
	{
		Document doc = new Document ();
		doc.add ( new TextField ( "title", keyword, Store.YES ) );
		doc.add ( new StringField ( "docId", id, Store.YES ) );
		//doc.add ( new StoredField ( "note", "An example of non-indexed field" ) );
		w.addDocument ( doc );
	}

	public ScoreDoc[] search ( QueryParser queryParser, String queryStr ) throws ParseException, IOException
	{
		return search ( queryParser.parse ( queryStr ) );
	}
	
	public ScoreDoc[] search ( Query query ) throws IOException
	{
		log.debug ( "query: {}", query.toString () );
		TopDocs topDocs = searcher.search ( query, 10 );
		return topDocs.scoreDocs;
	}

	public ScoreDoc[] searchByTitle ( String queryStr ) throws ParseException, IOException
	{
		return search ( new QueryParser ( "title", analyzer ), queryStr );
	}

	public ScoreDoc[] searchByID ( String id ) throws ParseException, IOException
	{
		QueryParser qp = new QueryParser ( "foo", analyzer );
		Query pq = qp.createPhraseQuery ( "docId", id );
		return search ( pq );
	}
		
	public ScoreDoc[] searchAllFields ( String queryStr ) throws ParseException, IOException
	{
		return search ( new MultiFieldQueryParser ( new String [] { "title", "docId" }, analyzer ), queryStr );
	}
		
	public ScoreDoc[] logResults ( ScoreDoc[] searchResults ) throws IOException
	{
		for ( ScoreDoc scoreDoc: searchResults )
		{
			Document doc = searcher.doc ( scoreDoc.doc );
			log.info ( "Document found: '{}', '{}', score: {}", doc.get ( "title" ), doc.get ( "docId" ), scoreDoc.score );
		}
		return searchResults;
	}	
}
