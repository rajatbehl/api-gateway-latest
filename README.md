# README #

API-Gateway service will be 

### What is this repository for? ###


API-Gateway service will act as a single point of contact for the client for communication with other services. This service will be responsible for below tasks
* Routing Calls

API-Gateway will be responsible for routing calls to other services. So this will have opportunity to modify the received request before forwarding to respective service and modify the response received from respective service before sending it to the client.
In some cases this service will also work as a orchestrator for e.g. in case of registering new user with the system this service will be responsible for following tasks :

	* call user service to create user.
	* call auth service to generate auth token for newly created user.
	* call wallet service to create user's wallet
	* and then prepare a single response for sending to the client.
	
So for some cases it may need to invoke more than one service to serve the client request.
We are using [Spring Cloud Gateway](https://docs.spring.io/spring-cloud-gateway/docs/2.2.5.RELEASE/reference/html/). Using this we can add/update/remove routes at runtime in [Consul](https://www.consul.io/) without have to restart the service instance.
Once the application comes up, it will look for the routes at path /API-Gateway/services . It is recommended to define the route for each service in its separate directory; for instance route for RAF service can be defined at /API-Gateway/services/raf/route where route will be the key and its value will defined in json format which will represent the org.springframework.cloud.gateway.route.RouteDefinition object expected by spring. Below is the sample example :

key : /API-Gateway/services/raf/route

value :
{
    "id": "raf",
    "uri": "http://localhost:8080",
    "predicates": [
        {
            "name": "Path",
            "args": {
                "regexp": "/raf/**"
            }
        }
    ],
    "filters": [
        {
            "name": "StripPrefix",
            "args": {
                "parts": 1
            }
        },
        {
            "name": "Authentication",
            "args": {
                "roles": "jwr-player"
            }
        }
    ]
}

Every route will be represented by a unique id "raf" for above case. For this route client will append /raf for calling raf service end-points. End-point will be http://api-gateway.jungleerummy.com/raf/invites . API-Gateway has one route defined which looks for /raf/** regex in request path and forward that request to raf service.
* Authentication/Authorization
 
 All the calls from client will get authenticated first before gateway forwards call to respective service. No other service has to worry about authenticating request.
 
 It is not recommended to maintain routes in application.yaml as suppose in feature if we want to disable that route we will not be able to as spring by defaults loads routes from application.yaml once refresh event is sent to refresh routes.
 
* Centralized Tracing

API-Gateway service will also support centralized tracing. Suppose if a client request needs to involve 4 different services and and one of them is not responsive or it is down, then we can track from logs to check issue is raised from which service


### How do I get set up? ###

* [Setup Redis Cluster](https://medium.com/@iamvishalkhare/create-a-redis-cluster-faa89c5a6bb4)
* Setup [Consul in developer mode](https://learn.hashicorp.com/tutorials/consul/get-started-install) for running locally.
* Once Consul is installed and running locally, configure below key/value pairs
key : /API-Gateway/spring.redis.cluster.nodes, value : 127.0.0.1:6001,127.0.0.1:6002,127.0.0.1:6003,127.0.0.1:6004,127.0.0.1:6005,127.0.0.1:6006
* Create /API-Gateway/services folder in consul, this is the location where api-gateway search for the routes for services. It is not mandatory to define. For example route for RAF service can be defined as

key : /API-Gateway/services/raf/route

value :
{
    "id": "raf
    "uri": "http://raf.jungleerummy.com
    "predicates": [
        {
            "name": "Path",
            "args": {
                "regexp": "/raf/**"
            }
        }
    ],
    "filters": [
        {
            "name": "RewritePath",
            "args": {
                "regexp": "/raf(?<segment>/?.*)",
                "replacement" : "$\\{segment}"
            }
        }
    ]
} 


### Who do I talk to? ###

* rajat.behl@jungleegames.com
* bharat.bhusan@jungleegames.com 