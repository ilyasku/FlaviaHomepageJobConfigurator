package jobposter.model;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.junit.Test;
import static org.junit.Assert.*;

public class MapperTest {
    
    public MapperTest() {
    }

    @Test
    public void testExtractVisibleJobsFromJson() throws Exception {
        String jsonString = "[\n"
                + "  {\n"
                + "    \"title\": \"Pizzabäcker*in (m/w)\",\n"
                + "    \"path\": \"test-job-2.html\",\n"
                + "    \"vacancyType\": \"office\"\n"
                + "  },\n"
                + "  {\n"
                + "    \"title\": \"Überdieschultergucker (m/w)\",\n"
                + "    \"path\": \"test-job-3.html\",\n"
                + "    \"vacancyType\": \"it\"\n"
                + "  }\n"
                + "]\n";
        
        List<String> visibleJobs = Mapper.extractVisibleJobsFromJson(jsonString);
        
        assertTrue(visibleJobs.size() == 2);
        assertTrue(visibleJobs.contains("test-job-2.html"));
        assertTrue(visibleJobs.contains("test-job-3.html"));        
    }
    
    @Test
    public void testUpdateJobByHtmlContent() {
        System.out.println("=================================================");
        System.out.println("test method: testUpdateJobByHtmlContent");
        String htmlContent = "<span>\n"
                + "<h2 data-job-type=\"office\">Pizzabäcker*in (m/w)</h2>"
                + "<p>Du suchst einen kreativen Job? Du willst handwerklich arbeiten und\n"
                + "gleichzeitig mit neuester Technologie. Dann komm zu uns!\n"
                + "Wir suchen einen aufgeschlossenen Senior Pizzaentwickler.\n"
                + "Als erstes müsstest du allerdings dein eigenes hausinternes\n"
                + " Pizzabestellsystem entwickeln.</p>\n"
                + "</span>";
        
        
        Job job = new Job();
        
        Mapper.updateJobByHtmlContent(job, htmlContent);
        
        System.out.println(job.getTitle());
        
        assertTrue(job.getTitle().equals("Pizzabäcker*in (m/w)"));
        assertTrue(job.getJobType() == JobType.OFFICE);
        assertTrue(job.getHtmlContent().equals(htmlContent));
        
    }    
    
    @Test
    public void testBuildJsonNodeOfVisibleJobs() {
        System.out.println("=================================================");
        System.out.println("test method: testBuildJsonNodeOfVisibleJobs");
        
        Map<String, Job> jobs = new HashMap<>();
        
        Job job1 = new Job();
        job1.setHtmlFileKey("job1.html");
        job1.setJobType(JobType.OFFICE);
        job1.setTitle("Job 1 (m/w)");
        
        Job job2 = new Job();
        job2.setHtmlFileKey("job2.html");
        job2.setJobType(JobType.IT);
        job2.setTitle("Job 2 (m/w)");
        
        Job job3 = new Job();
        job3.setHtmlFileKey("job3.html");
        job3.setJobType(JobType.OFFICE);
        job3.setTitle("Job 3 (m/w)");
        
        Job job4 = new Job();
        job4.setHtmlFileKey("job4.html");
        job4.setJobType(JobType.IT);
        job4.setTitle("Job 4 (m/w)");

        
        jobs.put(job1.getHtmlFileKey(), job1);
        jobs.put(job2.getHtmlFileKey(), job2);
        jobs.put(job3.getHtmlFileKey(), job3);
        jobs.put(job4.getHtmlFileKey(), job4);
        
        List<String> visibleJobs = new ArrayList<>();
        visibleJobs.add(job1.getHtmlFileKey());
        visibleJobs.add(job4.getHtmlFileKey());
                
        ArrayNode jsonNodeOfVisibleJobs = Mapper.buildJsonNodeOfVisibleJobs(visibleJobs, jobs);
        
        JsonNode firstVisibleJobNode = jsonNodeOfVisibleJobs.get(0);
                
        assertTrue("job1.html".equals(firstVisibleJobNode.get("path").asText()));
        assertTrue("office".equals(firstVisibleJobNode.get("vacancyType").asText()));
        assertTrue("Job 1 (m/w)".equals(firstVisibleJobNode.get("title").asText()));
        
        JsonNode secondVisibleJobNode = jsonNodeOfVisibleJobs.get(1);
        
        assertTrue("job4.html".equals(secondVisibleJobNode.get("path").asText()));
        assertTrue("it".equals(secondVisibleJobNode.get("vacancyType").asText()));
        assertTrue("Job 4 (m/w)".equals(secondVisibleJobNode.get("title").asText()));
    }
    
    @Test
    public void testInsertJobTypeIntoHtmlString() {
        System.out.println("=================================================");
        System.out.println("test method: testInsertJobTypeIntoHtmlString");
        
        String htmlStringWithoutJobType = "<h2>Job 1 (m/w)</h2><br>"
                + "<p>Some description of Job 1!</p>";
        
        String afterInsertHtmlStringWithoutJobType = Mapper.insertJobTypeIntoHtmlString(htmlStringWithoutJobType, JobType.OFFICE);        
        
        Document htmlDoc = Jsoup.parse(afterInsertHtmlStringWithoutJobType);
        Element heading = htmlDoc.select("h0, h1, h2, h3, h4, h5, h6").first();        
        String attr = heading.attr("data-job-type");
        Element description = htmlDoc.select("p").first();
        
        assertTrue("office".equals(attr));
        assertTrue("Job 1 (m/w)".equals(heading.text()));
        assertTrue("Some description of Job 1!".equals(description.text()));
        
        
        String htmlStringWithDifferentJobType = "<h1 data-job-type=\"office\">Job 2 (m/w)</h1><br>"
                + "<p>Some description of Job 2!</p>";
        
        String afterInsertHtmlStringWithDifferentJobType = Mapper.insertJobTypeIntoHtmlString(htmlStringWithDifferentJobType, JobType.IT);
        
        htmlDoc = Jsoup.parse(afterInsertHtmlStringWithDifferentJobType);
        heading = htmlDoc.select("h0, h1, h2, h3, h4, h5, h6").first();
        attr = heading.attr("data-job-type");
        description = htmlDoc.select("p").first();
        
        assertTrue("it".equals(attr));
        assertTrue("Job 2 (m/w)".equals(heading.text()));
        assertTrue("Some description of Job 2!".equals(description.text()));
        
        String htmlStringWithIdenticalJobType = "<h3 data-job-type=\"it\">Job 3 (m/w)</h3><br>"
                + "<p>Some description of Job 3!</p>";
        
        String afterInsertHtmlStringWithIdenticalJobType = Mapper.insertJobTypeIntoHtmlString(htmlStringWithIdenticalJobType, JobType.IT);
        
        htmlDoc = Jsoup.parse(afterInsertHtmlStringWithIdenticalJobType);
        heading = htmlDoc.select("h0, h1, h2, h3, h4, h5, h6").first();
        attr = heading.attr("data-job-type");
        description = htmlDoc.select("p").first();
        
        assertTrue("it".equals(attr));
        assertTrue("Job 3 (m/w)".equals(heading.text()));
        assertTrue("Some description of Job 3!".equals(description.text()));
        
    }
}
