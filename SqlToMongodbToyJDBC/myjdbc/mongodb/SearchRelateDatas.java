package myjdbc.mongodb;

import java.util.List;

import org.bson.Document;

import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;

public class SearchRelateDatas {
	public List<String> columns;
	public Document searchCondition;
	public MongoCollection<Document> collection;
	public FindIterable<Document> founds;

}
