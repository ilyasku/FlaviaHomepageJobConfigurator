package jobposter.model;

import com.amazonaws.services.s3.model.S3ObjectInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class ReaderS3ObjectContent {

    public static String InputStreamToString(S3ObjectInputStream stream) throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(stream));
        String returnString = "";
        while (true){
            String line = reader.readLine();
            if (line == null) break;
            returnString += line + "\n";
        }
        return returnString;
    }
    
}
