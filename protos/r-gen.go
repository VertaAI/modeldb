package main

import (
	"encoding/json"
	"flag"
	"io/ioutil"
	"os"
	"path"
	"text/template"

	"github.com/Masterminds/sprig"
)

var inputFile = flag.String("input", "", "run a database migration")
var outputFile = flag.String("output", "", "run a database migration")
var templateFile = flag.String("template", "", "run a database migration")

func main() {
	flag.Parse()

	tmpl, err := template.New(path.Base(*templateFile)).Funcs(sprig.TxtFuncMap()).ParseFiles(*templateFile)
	if err != nil {
		panic(err)
	}
	inputFile, err := os.Open(*inputFile)
	if err != nil {
		panic(err)
	}
	inputContent, err := ioutil.ReadAll(inputFile)
	if err != nil {
		panic(err)
	}
	var content map[string]interface{}
	err = json.Unmarshal(inputContent, &content)
	if err != nil {
		panic(err)
	}
	outputFile, err := os.OpenFile(*outputFile, os.O_RDWR|os.O_CREATE, 0755)
	if err != nil {
		panic(err)
	}
	err = tmpl.Execute(outputFile, content)
	if err != nil {
		panic(err)
	}
}
