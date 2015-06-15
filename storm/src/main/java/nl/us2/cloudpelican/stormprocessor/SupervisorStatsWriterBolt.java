package nl.us2.cloudpelican.stormprocessor;

/**
 * Created by robin on 07/06/15.
 */

import backtype.storm.Config;
import backtype.storm.task.OutputCollector;
import backtype.storm.task.TopologyContext;
import backtype.storm.topology.OutputFieldsDeclarer;
import backtype.storm.topology.base.BaseRichBolt;
import backtype.storm.tuple.Tuple;
import org.apache.commons.codec.binary.Base64;
import org.apache.storm.http.HttpResponse;
import org.apache.storm.http.client.HttpClient;
import org.apache.storm.http.client.methods.HttpPut;
import org.apache.storm.http.entity.StringEntity;
import org.apache.storm.http.impl.client.HttpClientBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import storm.starter.util.TupleHelpers;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 *
 * @author robin
 */
public class SupervisorStatsWriterBolt extends BaseRichBolt {

    OutputCollector _collector;
    HashMap<String, SupervisorFilterStats> resultAggregator;
    private HashMap<String, String> settings;

    private static final Logger LOG = LoggerFactory.getLogger(SupervisorStatsWriterBolt.class);

    public SupervisorStatsWriterBolt(HashMap<String, String> settings) {
        super();
        this.settings = settings;
    }

    public void prepare(Map conf, TopologyContext context, OutputCollector collector) {
        _collector = collector;
        resultAggregator = new HashMap<String, SupervisorFilterStats>();
    }

    public void execute(Tuple tuple) {
        if (TupleHelpers.isTickTuple(tuple)) {
            executeTick();
        } else {
            executeTuple(tuple);
        }
        _collector.ack(tuple);
    }

    public void executeTick() {
        _flush();
    }

    protected void _flush() {
        for (Map.Entry<String, SupervisorFilterStats> kv : resultAggregator.entrySet()) {
            System.out.println(kv.getKey() + " = " + kv.getValue().getCount());
            /*
            try {
                HttpClient client = HttpClientBuilder.create().build();

                String url = settings.get("supervisor_host") + "filter/" + kv.getKey() + "/stats";
                LOG.info(url);
                HttpPut put = new HttpPut(url);
                StringBuilder sb = new StringBuilder();
                for (String line : kv.getValue()) {
                    sb.append(line).append("\n");
                }
                StringEntity entity = new StringEntity(sb.toString());
                put.setEntity(entity);
                String token = new String(Base64.encodeBase64((settings.get("supervisor_username") + ":" + settings.get("supervisor_password")).getBytes()));
                LOG.debug(token);
                put.setHeader("Authorization", "Basic " + token);
                HttpResponse resp = client.execute(put);
                int status = resp.getStatusLine().getStatusCode();
                if (status >= 400) {
                    throw new Exception("Invalid status " + status);
                }
            } catch (Exception e) {
                LOG.error("Failed to write data to supervisor", e);
            }*/
        }
        resultAggregator.clear();
    }

    public Map<String, Object> getComponentConfiguration() {
        Config conf = new Config();
        int tickFrequencyInSeconds = 1;
        conf.put(Config.TOPOLOGY_TICK_TUPLE_FREQ_SECS, tickFrequencyInSeconds);
        return conf;
    }


    public void executeTuple(Tuple tuple) {
        String filterId = tuple.getStringByField("filter_id");
        int metric = tuple.getIntegerByField("metric");
        long increment = tuple.getLongByField("increment");


        Date d = new Date();
        long ts = d.getTime() / 1000L; // UNIX TS
        long bucket = ts - (ts % 60); // Minutely buckets
        String k = "f_" + filterId + "_m" + metric +"_b" + bucket;


        // Append in-memory
        if (!resultAggregator.containsKey(k)) {
            resultAggregator.put(k, new SupervisorFilterStats(filterId, metric, bucket));
        }
        resultAggregator.get(k).increment(increment);

        // No ack, is handled in outer
    }

    public void declareOutputFields(OutputFieldsDeclarer declarer) {
    }
}