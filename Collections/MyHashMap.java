
public class MyHashMap {
	
	private static class MyKey {
	    private final String key;
	    
	    MyKey(String key) {
	        this.key = key;
	    }
	    
	    public int hashCode() {
	        return 5;
	    }
	    
	    public boolean equals(Object other) {
	        return (other instanceof MyKey) && key.equals(((MyKey)other).key);
	    }
	}
	
	private static class Node{
		public Node next;
		public Object key;
		public Object value;
	}

	
	private Node root = null;
	private int nodeTotal = 0;
	private Node[] objArray;
	
	public MyHashMap(){
		this(16);
	}
	
	public MyHashMap(int maxSize){
		objArray = new Node[maxSize];
	}
	
	public void put(Object k, Object v) throws Exception {
		// calucate the hash key 
		if(k==null) throw new Exception("key is null");
		int pos = Math.abs(k.hashCode())%objArray.length;
		
		Node n = new Node();
		n.key = k;
		n.value = v;
		if(objArray[pos]==null){
			objArray[pos] = n;
		}else{
			Node tempNode = objArray[pos];
			while(tempNode.next!=null){
				tempNode = tempNode.next;
			}
			tempNode.next = n;
		}
		
	}
	
	public Object get(Object k) throws Exception {
		if(k==null) throw new Exception("key is null");
		int pos = Math.abs(k.hashCode())%objArray.length;
		if(objArray[pos]!=null){
			Node tempNode = objArray[pos];
			while(tempNode!=null){
				if(tempNode.key.equals(k)){
					return tempNode.value;
				}
				tempNode = tempNode.next;
			}
		}
		return null;
	}
	
	public boolean contains(Object k) throws Exception {
		if(k==null) throw new Exception("key is null");
		int pos = Math.abs(k.hashCode())%objArray.length;
		if(objArray[pos]!=null){
			Node tempNode = objArray[pos];
			while(tempNode!=null){
				if(tempNode.key.equals(k)){
					return true;
				}
				tempNode = tempNode.next;
			}
		}
		return false;
	}
	
	public Object remove(Object k) throws Exception {
		if(k==null) throw new Exception("key is null");
		int pos = Math.abs(k.hashCode())%objArray.length;
		if(objArray[pos]!=null){
			Node tempNode = objArray[pos], prevNode = null;
			while(tempNode!=null){
				if(tempNode.key.equals(k)){
					if(prevNode==null){
						objArray[pos] = tempNode.next;
					}else{
						prevNode.next = tempNode.next;
					}
					return tempNode.value;
				}
				prevNode = tempNode;
				tempNode = tempNode.next;
			}
		}
		return null;
	}
	
	public void clear(){
		for(int i=0; i<objArray.length; ++i){
			objArray[i] = null;
		}
	}
	
	public static void main(String[] args) throws Exception {
		/*
		String k1 = "key001"; String v1 = "value1";
		String k2 = "key002"; String v2 = "value2";
		MyHashMap testMap = new MyHashMap();
		testMap.put(k1, v1);
		testMap.put(k2, v2);
		System.out.println(testMap.get(k1));
		System.out.println(testMap.get(k2));
		*/
		

		MyKey k1 = new MyKey("key001"); String v1 = "value1";
		MyKey k2 = new MyKey("key002"); String v2 = "value2";
		MyHashMap testMap = new MyHashMap();
		testMap.put(k1, v1);
		testMap.put(k2, v2);
		System.out.println(testMap.get(k1));
		System.out.println(testMap.get(k2));
		System.out.println(testMap.remove(k2));
		System.out.println(testMap.get(k2));
		
		
	}
	
	

}
