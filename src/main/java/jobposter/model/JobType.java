package jobposter.model;

public enum JobType {
    IT("it"), OFFICE("office");
    
    private String stringIdentifier;
    
    JobType(String stringIdentifier){
        this.stringIdentifier = stringIdentifier;
    }
    
    public String getStringIdentifier(){
        return stringIdentifier;
    }
}