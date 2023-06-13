package main

import (
	"crypto/tls"
	"fmt"
	"log"
	"os"
	"time"

	mqtt "github.com/eclipse/paho.mqtt.golang"
)

const (
	clientID = "client1-session1"
	fqdn     = "mqtt-demo.westeurope-1.ts.eventgrid.azure.net"
	topic    = "contosotopics/topic1"
	v311     = 4
)

func handler(client mqtt.Client, msg mqtt.Message) {
	log.Printf("Topic: %s", msg.Topic())
	log.Printf("Message: %s", msg.Payload())
}

func newTLSConfig() (*tls.Config, error) {
	cert, err := tls.LoadX509KeyPair("./example.org.crt", "./example.org.key")
	if err != nil {
		return nil, err
	}
	return &tls.Config{
		InsecureSkipVerify: true,
		Certificates:       []tls.Certificate{cert},
	}, nil
}

func newClientOptions(tlsConfig *tls.Config) *mqtt.ClientOptions {
	uri := fmt.Sprintf("ssl://%s:8883", fqdn)
	opts := mqtt.NewClientOptions()
	opts.AddBroker(uri)
	opts.SetClientID(clientID)
	opts.SetDefaultPublishHandler(handler)
	opts.SetUsername("client1-authnID")
	opts.SetPassword("")
	opts.SetTLSConfig(tlsConfig)
	opts.SetProtocolVersion(v311)
	opts.OnConnect = func(c mqtt.Client) {
		log.Printf("Connected to %s", uri)
	}
	return opts
}

func main() {
	mqtt.ERROR = log.New(os.Stdout, "", 0)
	mqtt.CRITICAL = log.New(os.Stdout, "", 0)

	tlsConfig, err := newTLSConfig()
	if err != nil {
		log.Fatal(err)
	}
	opts := newClientOptions(tlsConfig)
	c := mqtt.NewClient(opts)
	if token := c.Connect(); token.Wait() && token.Error() != nil {
		log.Fatal(token.Error())
	}

	if token := c.Subscribe(topic, 0, nil); token.Wait() && token.Error() != nil {
		log.Fatal(token.Error())
	}

	for i := 0; i < 5; i++ {
		text := fmt.Sprintf("this is msg #%d!", i)
		token := c.Publish(topic, 0, false, text)
		token.Wait()
	}

	time.Sleep(6 * time.Second)

	if token := c.Unsubscribe(topic); token.Wait() && token.Error() != nil {
		log.Fatal(token.Error())
	}

	log.Println("Disconnecting...")
	c.Disconnect(250)
	log.Println("Done")
}
