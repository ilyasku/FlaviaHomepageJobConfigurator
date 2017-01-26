# FlaviaJobPoster

Tool to handle posting and editing jobs shown at the company's [career page](https://www.flavia-it.de/karriere.html).  
Jobs are read from and written to a Amazon S3 Bucket as individual HTML files. 

## Project Structure 

Gradle is used as build tool for this project.  
Source code found in the `src` folder contains some classes to handle communication 
with the S3 Bucket and map content of job html files to Job objects and vice versa.  
A graphical user interface is placed in the (gradle) subproject folder `GUI`. 
If you have JDK 8 with JavaFX 2 installed (e.g. OpenJDK 1.8 with OpenJFX 8) you
should be able to start the GUI. Open a terminal, navigate to the project's root folder and 
type `./gradlew jfxRun`. This requires you to have your valid AWS credentials in a file at 
`~/.aws/credentials`.