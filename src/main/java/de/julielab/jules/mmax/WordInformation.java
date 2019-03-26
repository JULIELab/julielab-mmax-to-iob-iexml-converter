package de.julielab.jules.mmax;

import java.util.LinkedList;
import java.util.List;

public class WordInformation {
	private String id;
	private int position;
	private String text;
	private List<MarkableContainer> markables;
	private MarkableContainer sentence;
	private boolean followedBySpace;
	
	public WordInformation() {
		followedBySpace=true;
	}
	
	public void addMarkable(MarkableContainer container){
		if(markables==null){
			markables=new LinkedList<MarkableContainer>();
		}
		markables.add(container);
	}
	
	public int getPosition() {
		return position;
	}
	public void setPosition(int position) {
		this.position = position;
	}
	public String getText() {
		return text;
	}
	public void setText(String text) {
		this.text = text;
	}
	public List<MarkableContainer> getMarkables() {
		if(markables==null){
			markables=new LinkedList<MarkableContainer>();
		}
		return markables;
	}
	public void setMarkables(List<MarkableContainer> markables) {
		this.markables = markables;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getId() {
		return id;
	}

	@Override
	public String toString() {
		return "WordInformation [id=" + id + ", markables=" + markables + ", position=" + position + ", text=" + text + "]";
	}

	public void setSentence(MarkableContainer c) {
		this.sentence=c;		
	}
	
	public MarkableContainer getSentence(){
		return this.sentence;
	}

	public boolean isFollowedBySpace() {
		return this.followedBySpace;
	}

	public void setFollowedBySpace(boolean followedBySpace) {
		this.followedBySpace = followedBySpace;
	}
}
