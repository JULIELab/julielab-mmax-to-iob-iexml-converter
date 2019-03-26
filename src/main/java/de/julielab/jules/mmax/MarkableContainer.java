package de.julielab.jules.mmax;


public class MarkableContainer {
	private int begin;
	private int end;
	private String id = "";
	private String lable = "";
	private int priority;
	private boolean ignore = false;
	
	public int getBegin() {
		return begin;
	}
	public void setBegin(int begin) {
		this.begin = begin;
	}
	public int getEnd() {
		return end;
	}
	public void setEnd(int end) {
		this.end = end;
	}
	public String getId() {
		return id;
	}
	public void setId(String id) {
		this.id = id;
	}
	public String getLable() {
		return lable;
	}
	public void setLable(String lable) {
		this.lable = lable;
	}
	
	@Override
	public boolean equals(Object obj) {
		if(obj instanceof MarkableContainer){
			return id==((MarkableContainer)obj).id;
		}
		return super.equals(obj);
	}
	public int getPriority() {
		return this.priority;
	}
	public void setPriority(int priority) {
		this.priority = priority;
	}
	public void setIgnore(boolean ignore) {
		if(!this.ignore && ignore){
			Statistics.removeType(this);
			this.ignore = ignore;
		}
	}
	public boolean isIgnore() {
		return ignore;
	}
	@Override
	public String toString() {
		return "MarkableContainer [id=" + id + ", begin=" + begin + ", end=" + end + ", ignore=" + ignore + ", lable=" + lable + ", priority=" + priority + "]";
	}
	
	
}
