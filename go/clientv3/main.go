package main

import (
	"context"
	"crypto/tls"
	"flag"
	"fmt"
	"log"
	"os"
	"os/signal"
	"syscall"
	"time"

	mqtt "github.com/eclipse/paho.mqtt.golang"
)

const (
	v311 = 4
)

func handler(client mqtt.Client, msg mqtt.Message) {
	log.Printf("Topic: %s", msg.Topic())
	log.Printf("Message: %s", msg.Payload())
}

func newTLSConfig(certFile, keyFile string) (*tls.Config, error) {
	cert, err := tls.LoadX509KeyPair(certFile, keyFile)
	if err != nil {
		return nil, err
	}
	return &tls.Config{
		InsecureSkipVerify: true,
		Certificates:       []tls.Certificate{cert},
	}, nil
}

func newClientOptions(fqdn string, username string, clientID string, tlsConfig *tls.Config) *mqtt.ClientOptions {
	uri := fmt.Sprintf("ssl://%s:8883", fqdn)
	opts := mqtt.NewClientOptions()
	opts.AddBroker(uri)
	opts.SetClientID(clientID)
	opts.SetDefaultPublishHandler(handler)
	opts.SetUsername(username)
	opts.SetPassword("") // we use a client certificate instead
	opts.SetTLSConfig(tlsConfig)
	opts.SetProtocolVersion(v311)
	opts.OnConnect = func(c mqtt.Client) {
		log.Printf("Connected to %s", uri)
	}
	return opts
}

func main() {
	subscribe := flag.Bool("subscribe", false, "subscribe to the topic")
	publish := flag.Bool("publish", true, "publish messages to the topic")
	flag.Parse()

	if !*subscribe && !*publish {
		log.Fatal("At least one of -subscribe or -publish must be set")
	}

	fqdn, ok := os.LookupEnv("MQTT_BROKER_FQDN")
	if !ok {
		log.Fatal("MQTT_BROKER_FQDN environment variable is not set")
	}
	topic, ok := os.LookupEnv("MQTT_TOPIC")
	if !ok {
		log.Fatal("MQTT_TOPIC environment variable is not set")
	}
	username, ok := os.LookupEnv("MQTT_USERNAME")
	if !ok {
		log.Fatal("MQTT_USERNAME environment variable is not set")
	}
	clientID, ok := os.LookupEnv("MQTT_CLIENT_ID")
	if !ok {
		log.Fatal("MQTT_CLIENT_ID environment variable is not set")
	}
	tlsCertFile, ok := os.LookupEnv("MQTT_TLS_CERT_FILE")
	if !ok {
		log.Fatal("MQTT_TLS_CERT_FILE environment variable is not set")
	}
	tlsKeyFile, ok := os.LookupEnv("MQTT_TLS_KEY_FILE")
	if !ok {
		log.Fatal("MQTT_TLS_KEY_FILE environment variable is not set")
	}

	mqtt.ERROR = log.New(os.Stdout, "", 0)
	mqtt.CRITICAL = log.New(os.Stdout, "", 0)

	tlsConfig, err := newTLSConfig(tlsCertFile, tlsKeyFile)
	if err != nil {
		log.Fatal(err)
	}
	opts := newClientOptions(fqdn, username, clientID, tlsConfig)
	c := mqtt.NewClient(opts)
	if token := c.Connect(); token.Wait() && token.Error() != nil {
		log.Fatal(token.Error())
	}

	ctx, stop := signal.NotifyContext(context.Background(), os.Interrupt, syscall.SIGTERM)
	defer stop()

	if *subscribe {
		fmt.Printf("Subscribing to topic %s. Press Ctrl+C to exit.", topic)
		if token := c.Subscribe(topic, 0, nil); token.Wait() && token.Error() != nil {
			log.Fatal(token.Error())
		}
	}

	if *publish {
		go func() {
			fmt.Println("Sending messages. Press Ctrl+C to exit.")
			i := 0
			for {
				text := fmt.Sprintf("this is msg #%d!", i)
				token := c.Publish(topic, 0, false, text)
				token.Wait()
				time.Sleep(2 * time.Second)
				i++
			}
		}()
	}

	<-ctx.Done()
	fmt.Println("Stopped...")

	if *subscribe {
		if token := c.Unsubscribe(topic); token.Wait() && token.Error() != nil {
			log.Fatal(token.Error())
		}
	}

	fmt.Println("Disconnecting...")
	c.Disconnect(250)
	fmt.Println("Done")
}
