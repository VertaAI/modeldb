package models

type NetworkCommitColor struct {
	Color  int `json:"color"`
	Commit *Commit
}
