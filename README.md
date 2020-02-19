# PJUG MONITORING DEMO
1. Install Docker
2. Run graphite: \
```docker run -d --name graphite-server -p 80:80 -p 2003-2004:2003-2004 -p 2023-2024:2023-2024 -p 8125:8125/udp -p 8126:8126 -p 8080:8080 graphiteapp/graphite-statsd``` 
3. Run Graphana:\
```docker run -d --name graphana-server -p 3000:3000 grafana/grafana```
4. Run ELK stack \
Clone repo: \
```git clone https://github.com/deviantony/docker-elk.git``` \
Add config file to ../logstash/pipeline: \
```input {
       file {
           path => "/usr/share/logback/*.log"
           codec => "json"
           type => "logback"
   				start_position => "beginning"
       }
   }
   
   output {
       if [type]=="logback" {
            elasticsearch {
                hosts => [ "elasticsearch:9200" ]
                index => "demoapp"
   						 user => "elastic"
   						 password => "changeme"
           }
   			  stdout { codec => rubydebug }
       }
   }
```
Run docker-compose: ```docker-compose up```
