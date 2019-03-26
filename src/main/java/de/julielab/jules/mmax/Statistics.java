package de.julielab.jules.mmax;


import java.util.HashMap;

public class Statistics {
	public static int sentences=0;
	public static int acceptedTypes=0;
	public static int projects=0;
	public static int segmentet_Types=0;
	public static int overlapping_Types=0;
	public static int types_Without_Sentence=0;

	public static HashMap <String,Integer> labels= new HashMap<String, Integer>();
	
	public static String getStatistitcs(){
		String s="Statistics:\n";
		s+="Projects: "+projects +"\n";
		s+="Sentence Annotations: "+sentences +"\n";
		s+="Entity Annotations (total): "+(acceptedTypes+segmentet_Types+overlapping_Types) +"\n";
		s+="Accepted Entity Annotations: "+(acceptedTypes) +"\n";
		s+="Skipped Discontinuous Entity Annotations: " + segmentet_Types +"\n";
		s+="Skipped Overlapping Entity Annotation: "+ overlapping_Types +"\n";
		for(String i:labels.keySet().toArray(new String[labels.size()])){
			s+= "Entity Annotations of type " +i+ ": " + labels.get(i) + "\n";
		}
		//s+="Skipped annotations without a Sentence: "+types_Without_Sentence +"\n";
		return s;
	}

	public synchronized static void addType(MarkableContainer type) {
		Statistics.acceptedTypes++;
		Integer i = labels.get(type.getLable());
		if(i==null)
			labels.put(type.getLable(), 1);
		else
			labels.put(type.getLable(), i+1);
	}

	public synchronized static void removeType(MarkableContainer type) {
		Statistics.overlapping_Types++;
		Statistics.acceptedTypes--;
		Integer i = labels.get(type.getLable());
		if(i==null)
			labels.put(type.getLable(), 1);
		else
			labels.put(type.getLable(), i-1);
	}
	
}
