package pagination

import (
	"context"

	"github.com/VertaAI/modeldb/backend/graphql/internal/codec"
	"github.com/VertaAI/modeldb/backend/graphql/internal/schema"
	"github.com/VertaAI/modeldb/backend/graphql/internal/schema/errors"
	"go.uber.org/zap"
)

type Next struct {
	PageNumber int `json:"page_number"`
	PageLimit  int `json:"page_limit"`
	done       bool
}

func NewNext(logger *zap.Logger, ctx context.Context, next *string, query *schema.PaginationQuery) (*Next, error) {
	p := &Next{
		PageNumber: 1,
		PageLimit:  100,
	}

	if next != nil && query != nil {
		return nil, errors.NextOrQuery(ctx)
	} else if next != nil {
		if err := p.Decode(*next); err != nil {
			logger.Error("failed to decode next token", zap.Error(err))
			return nil, errors.InvalidNextToken(ctx)
		}
	} else if query != nil {
		if query.Limit != nil && 0 < *query.Limit && *query.Limit < p.PageLimit {
			p.PageLimit = *query.Limit
		}
		if query.Page != nil {
			p.PageNumber = *query.Page
		}
	}

	return p, nil
}

func (n *Next) Decode(next string) error {
	return codec.Decode(next, n)
}

func (n *Next) Encode() *string {
	if n.done {
		return nil
	}

	s := codec.Encode(n)
	return &s
}

func (n *Next) ProcessResponse(totalRecords int64) {
	if totalRecords <= int64(n.PageNumber*n.PageLimit) {
		n.done = true
	}
	n.PageNumber++
}
