package au.org.emii.aggregationworker;

import au.org.emii.aggregator.NetcdfAggregator;
import au.org.emii.aggregator.exception.AggregationException;
import au.org.emii.aggregator.exception.SubsetException;
import au.org.emii.aggregator.overrides.AggregationOverrides;
import au.org.emii.aggregator.overrides.AggregationOverridesReader;
import au.org.emii.download.Download;
import au.org.emii.download.DownloadConfig;
import au.org.emii.download.DownloadRequest;
import au.org.emii.download.Downloader;
import au.org.emii.download.ParallelDownloadManager;
import com.amazonaws.auth.DefaultAWSCredentialsProviderChain;
import com.amazonaws.services.s3.transfer.TransferManager;
import com.amazonaws.services.s3.transfer.Upload;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.stereotype.Component;
import thredds.crawlabledataset.s3.S3URI;
import ucar.ma2.Range;
import ucar.nc2.time.CalendarDate;
import ucar.nc2.time.CalendarDateRange;
import ucar.unidata.geoloc.LatLonPoint;
import ucar.unidata.geoloc.LatLonPointImmutable;
import ucar.unidata.geoloc.LatLonRect;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class AggregationRunner implements CommandLineRunner {

    private static final Logger logger = LoggerFactory.getLogger(au.org.emii.aggregationworker.AggregationRunner.class);

    @Override
    public void run(String... args) {
        try {
            Options options = new Options();

            options.addOption("b", true, "restrict to bounding box specified by left lower/right upper coordinates e.g. -b 120,-32,130,-29");
            options.addOption("z", true, "restrict data to specified z index range e.g. -z 2,4");
            options.addOption("t", true, "restrict data to specified date/time range in ISO 8601 format e.g. -t 2017-01-12T21:58:02Z,2017-01-12T22:58:02Z");
            // reexamine how config would be passed to program - as an argument perhaps?
            options.addOption("c", true, "aggregation overrides to be applied - xml");

            CommandLineParser parser = new DefaultParser();
            CommandLine line = parser.parse(options, args);

            URL url = new URL(line.getArgs()[0]);
            URLConnection conn = url.openConnection();

            List<String> inputFiles = new ArrayList<>();

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
                inputFiles = reader.lines().collect(Collectors.toList());
            }

            String destination = line.getArgs()[1];
            S3URI s3URI = new S3URI(destination);

            Set<DownloadRequest> downloads = new LinkedHashSet<>();

            for (String inputFile: inputFiles.subList(1, inputFiles.size()-1)) {
                if (inputFile.trim().isEmpty()) continue;
                URL fileUrl = new URL("http://s3-ap-southeast-2.amazonaws.com/imos-data/" + inputFile.split(",")[1]);
                long size = Long.parseLong(inputFile.split(",")[2]);
                downloads.add(new DownloadRequest(fileUrl, size));
            }

            String bboxArg = line.getOptionValue("b");
            String zSubsetArg = line.getOptionValue("z");
            String timeArg = line.getOptionValue("t");
            String overridesArg = line.getOptionValue("c");

            LatLonRect bbox = null;

            if (bboxArg != null) {
                String[] bboxCoords = bboxArg.split(",");
                double minLon = Double.parseDouble(bboxCoords[0]);
                double minLat = Double.parseDouble(bboxCoords[1]);
                double maxLon = Double.parseDouble(bboxCoords[2]);
                double maxLat = Double.parseDouble(bboxCoords[3]);
                LatLonPoint lowerLeft = new LatLonPointImmutable(minLat, minLon);
                LatLonPoint upperRight = new LatLonPointImmutable(maxLat, maxLon);
                bbox = new LatLonRect(lowerLeft, upperRight);
            }

            Range zSubset = null;

            if (zSubsetArg != null) {
                String[] zSubsetIndexes = zSubsetArg.split(",");
                int startIndex = Integer.parseInt(zSubsetIndexes[0]);
                int endIndex = Integer.parseInt(zSubsetIndexes[1]);
                zSubset = new Range(startIndex, endIndex);
            }

            CalendarDateRange timeRange = null;

            if (timeArg != null) {
                String[] timeRangeComponents = timeArg.split(",");
                CalendarDate startTime = CalendarDate.parseISOformat("Gregorian", timeRangeComponents[0]);
                CalendarDate endTime = CalendarDate.parseISOformat("Gregorian", timeRangeComponents[1]);
                timeRange = CalendarDateRange.of(startTime, endTime);
            }

            AggregationOverrides overrides;

            if (overridesArg != null) {
                overrides = AggregationOverridesReader.load(Paths.get(overridesArg));
            } else {
                overrides = new AggregationOverrides(); // Use default (i.e. no overrides)
            }

            DownloadConfig downloadConfig = new DownloadConfig.ConfigBuilder().build();

            Downloader downloader = new Downloader(60000, 60000);

            Path outputFile = Files.createTempFile("agg", ".nc");

            try (
                ParallelDownloadManager downloadManager = new ParallelDownloadManager(downloadConfig, downloader);
                NetcdfAggregator netcdfAggregator = new NetcdfAggregator(
                    outputFile, overrides, bbox, zSubset, timeRange)
            ){
                for (Download download : downloadManager.download(downloads)) {
                    netcdfAggregator.add(download.getPath());
                    downloadManager.remove();
                }

                logger.info("Copying output file to {}...", s3URI.toString());

                DefaultAWSCredentialsProviderChain credentialProviderChain = new DefaultAWSCredentialsProviderChain();
                TransferManager tx = new TransferManager(credentialProviderChain.getCredentials());
                Upload myUpload = tx.upload(s3URI.getBucket(), s3URI.getKey(), outputFile.toFile());
                myUpload.waitForCompletion();
                tx.shutdownNow();
            } finally {
                Files.deleteIfExists(outputFile);
            }
        } catch (Throwable e) {
            e.printStackTrace();
            System.exit(1);
        }
    }
}
