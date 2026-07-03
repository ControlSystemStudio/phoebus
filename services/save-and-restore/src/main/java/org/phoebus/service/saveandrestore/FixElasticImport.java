package org.phoebus.service.saveandrestore;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;

public class FixElasticImport {

    public static void main(String[] args){
        try {
            new FixElasticImport().doIt(args[0]);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void doIt(String inputFile) throws Exception{
        File in = new File(inputFile);
        File out = new File(in.getParentFile(), "out_" + in.getName());

        BufferedReader fileReader = new BufferedReader(new FileReader(in));
        FileWriter fileWriter = new FileWriter(out);

        int counter = 0;
        int size = 1000;
        int skip = 14000;
        for(int i = 0; i < skip; i++){
            fileReader.readLine();
        }
        while(counter < size){
            String line = fileReader.readLine();
            if(line == null){
                break;
            }
            int scoreIndex = line.indexOf(",\"_score");
            String instruction = "{\"index\": " + line.substring(0, scoreIndex) + "}}";
            String data = line.substring(scoreIndex + 22);
            data = data.substring(0, data.length() - 1);
            System.out.println(counter + " " + line.length() + " " + line.substring(0, 70));
            fileWriter.write(instruction + "\n" + data + "\n");
            counter++;
        }
        fileWriter.close();
        System.out.println(counter);
    }

}
