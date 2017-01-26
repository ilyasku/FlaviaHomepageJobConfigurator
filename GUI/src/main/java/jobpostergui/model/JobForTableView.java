package jobpostergui.model;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import jobposter.model.Job;

public class JobForTableView {
    private Job job;
    private StringProperty htmlFileKey;
    private BooleanProperty visible;
    private StringProperty htmlContent;
    
    public JobForTableView(Job job){       
        htmlFileKey = new SimpleStringProperty(job.getHtmlFileKey());
        visible = new SimpleBooleanProperty(job.isVisible());
        htmlContent = new SimpleStringProperty(job.getHtmlContent());
        this.job = job;    
    }    
    
    public void setHtmlFileKey(String htmlFileKey) {        
        this.htmlFileKey.set(htmlFileKey);
        if (job != null) {            
            job.setHtmlFileKey(htmlFileKey);
        }
    }
    
    public String getHtmlFileKey(){
        return htmlFileKey.get();
    }
    
    public StringProperty htmlFileKeyProperty() {
        return htmlFileKey;
    }
    
    public void setVisible(Boolean status) {
        visible.set(status);
        if (job != null) {
            job.setVisible(status);
        }
    }
    
    public boolean isVisible() {
        return visible.get();
    }
    
    public BooleanProperty visibleProperty() {
        return visible;
    }    
    
    public void setHtmlContent(String htmlContent){
        this.htmlContent.set(htmlContent);
        if (job != null) {
            job.setHtmlContent(htmlContent);
        }
    }
    
    public String getHtmlContent() {
        return htmlContent.get();
    }
    
    public StringProperty htmlContentProperty() {
        return htmlContent;
    }
    
    public Job getJob(){
        return job;
    }    
}
