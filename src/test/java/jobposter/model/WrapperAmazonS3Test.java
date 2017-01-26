package jobposter.model;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.ObjectListing;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.s3.model.S3ObjectInputStream;
import com.amazonaws.services.s3.model.S3ObjectSummary;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import org.junit.Test;
import org.mockito.Matchers;
import static org.mockito.Mockito.when;
import org.junit.Ignore;
import static org.mockito.Mockito.mock;
import org.apache.commons.io.IOUtils;
import static org.junit.Assert.assertTrue;
import org.junit.runner.RunWith;
import static org.mockito.BDDMockito.given;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;


@RunWith(PowerMockRunner.class)
public class WrapperAmazonS3Test {
    
    public WrapperAmazonS3Test() {
    }

    @Test
    public void testGetHtmlFileNames() {
        System.out.println("=================================================");
        System.out.println("test method: testGetHtmlFileNames");
        
        List<S3ObjectSummary> mockObjectSummaries = new ArrayList<>();
        
        S3ObjectSummary mockObjectSummary1 = mock(S3ObjectSummary.class);
        S3ObjectSummary mockObjectSummary2 = mock(S3ObjectSummary.class);
        S3ObjectSummary mockObjectSummary3 = mock(S3ObjectSummary.class);
        
        Mockito.doReturn("job1.html").when(mockObjectSummary1).getKey();
        Mockito.doReturn("job2.html").when(mockObjectSummary2).getKey();
        Mockito.doReturn("jobs.json").when(mockObjectSummary3).getKey();

        mockObjectSummaries.add(mockObjectSummary1);
        mockObjectSummaries.add(mockObjectSummary2);
        mockObjectSummaries.add(mockObjectSummary3);
        
        ObjectListing mockObjectListing = mock(ObjectListing.class);
        Mockito.doReturn(mockObjectSummaries).when(mockObjectListing).getObjectSummaries();
        
        AmazonS3 mockAmazonS3 = mock(AmazonS3.class);
        Mockito.doReturn(mockObjectListing).when(mockAmazonS3).listObjects(Matchers.anyString());
        
        WrapperAmazonS3 wrapper = new WrapperAmazonS3();
        wrapper.setAmazonS3(mockAmazonS3);
        wrapper.setBucketName("test-bucket");
        
        List<String> htmlFileNames = wrapper.getHtmlFileNames();
        
        assertTrue(2 == htmlFileNames.size());
        assertTrue(htmlFileNames.contains("job1.html"));
        assertTrue(htmlFileNames.contains("job2.html"));                
    }
    
    @PrepareForTest(ReaderS3ObjectContent.class)
    @Test
    public void testGetJob() throws IOException {
        System.out.println("=================================================");
        System.out.println("test method: testGetJob");
        
        PowerMockito.mockStatic(ReaderS3ObjectContent.class);
        PowerMockito.when(ReaderS3ObjectContent.InputStreamToString(Mockito.any()))
                .thenReturn("<h2 data-job-type=\"it\">Job 1 (m/w)</h2><br><p>Description of Job 1!</p>");
        
        S3ObjectInputStream mockS3ObjectInputStream = mock(S3ObjectInputStream.class);
        
        S3Object mockS3Object = mock(S3Object.class);
        Mockito.doReturn(mockS3ObjectInputStream).when(mockS3Object).getObjectContent();
        
        AmazonS3 mockAmazonS3 = mock(AmazonS3.class);
        Mockito.doReturn(mockS3Object).when(mockAmazonS3).getObject(Matchers.anyString(), Matchers.anyString());
                        
        WrapperAmazonS3 wrapper = new WrapperAmazonS3();
        wrapper.setAmazonS3(mockAmazonS3);
        wrapper.setBucketName("test-bucket");
        
        Job job = wrapper.getJob("job1.html");
        
        assertTrue("job1.html".equals(job.getHtmlFileKey()));
        assertTrue(JobType.IT == job.getJobType());
        assertTrue("Job 1 (m/w)".equals(job.getTitle()));
        assertTrue("<h2 data-job-type=\"it\">Job 1 (m/w)</h2><br><p>Description of Job 1!</p>".equals(job.getHtmlContent()));
    }
    
    @PrepareForTest(ReaderS3ObjectContent.class)
    @Test
    public void testGetVisibleJobs() throws IOException {
        System.out.println("=================================================");
        System.out.println("test method: testGetVisibleJobs");
        
        PowerMockito.mockStatic(ReaderS3ObjectContent.class);
        PowerMockito.when(ReaderS3ObjectContent.InputStreamToString(Mockito.any()))
                .thenReturn("[{\"title\":\"Spaßbremse (m/w)\","
                        + "\"path\":\"test-job-1.html\",\"vacancyType\":\"it\"},"
                        + "{\"title\":\"Go-getter (m/w)\","
                        + "\"path\":\"test-job-3.html\",\"vacancyType\":\"it\"}]");
        
        S3ObjectInputStream mockS3ObjectInputStream = mock(S3ObjectInputStream.class);
        
        S3Object mockS3Object = mock(S3Object.class);
        Mockito.doReturn(mockS3ObjectInputStream).when(mockS3Object).getObjectContent();
        
        AmazonS3 mockAmazonS3 = mock(AmazonS3.class);
        Mockito.doReturn(mockS3Object).when(mockAmazonS3).getObject(Matchers.anyString(), Matchers.anyString());
                        
        WrapperAmazonS3 wrapper = new WrapperAmazonS3();
        wrapper.setAmazonS3(mockAmazonS3);
        wrapper.setBucketName("test-bucket");
        
        List<String> listOfVisibleJobs = wrapper.getVisibleJobs();
        
        assertTrue(listOfVisibleJobs.contains("test-job-1.html"));
        assertTrue(listOfVisibleJobs.contains("test-job-3.html"));
    }    
}
