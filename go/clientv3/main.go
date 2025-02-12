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
	fmt.Printf("Topic: %s\n", msg.Topic())
	fmt.Printf("Message: %s\n", msg.Payload())
}

func newTLSConfig(certFile, keyFile string) (*tls.Config, error) {
	cert, err := tls.LoadX509KeyPair(certFile, keyFile)
	if err != nil {
		return nil, err
	}
	return &tls.Config{
		InsecureSkipVerify: false,
		Certificates:       []tls.Certificate{cert},
		ClientAuth:         tls.RequireAndVerifyClientCert,
	}, nil
}

func newClientOptions(fqdn string, username string, clientID string, tlsConfig *tls.Config) *mqtt.ClientOptions {
	uri := fmt.Sprintf("tls://%s:8883", fqdn)
	opts := mqtt.NewClientOptions()
	opts.AddBroker(uri)
	opts.SetClientID(clientID)
	opts.SetDefaultPublishHandler(handler)
	opts.SetUsername(username)
	opts.SetPassword("") // we use a client certificate instead
	opts.SetTLSConfig(tlsConfig)
	opts.SetProtocolVersion(v311)
	opts.SetCleanSession(false)
	opts.SetOnConnectHandler(func(c mqtt.Client) {
		fmt.Printf("Connected to %s\n", uri)
	})
	opts.SetResumeSubs(true)
	return opts
}

func main() {
	subscribe := flag.Bool("subscribe", false, "subscribe to the topic")
	publish := flag.Bool("publish", true, "publish messages to the topic")
	message := flag.String("message", "", "message to publish")
	flag.Parse()

	if !*subscribe && !*publish {
		log.Fatal("At least one of -subscribe or -publish must be set")
	}

	if *publish && *message == "" {
		log.Fatal("The -message flag must be set when -publish is set")
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
		fmt.Printf("Subscribing to topic %s. Press Ctrl+C to exit.\n", topic)
		if token := c.Subscribe(topic, 1, nil); token.Wait() && token.Error() != nil {
			log.Fatal(token.Error())
		}
	}

	if *publish {
		go func() {
			fmt.Println("Sending messages. Press Ctrl+C to exit.")
			i := 1
			for {
				text := fmt.Sprintf("%s #%d!", *message, i)
				token := c.Publish(topic, 1, false, text)
				token.Wait()
				time.Sleep(2 * time.Second)
				i++
			}
		}()
	}

	<-ctx.Done()

	fmt.Println("Disconnecting...")
	c.Disconnect(250)
	fmt.Println("Done")
}
