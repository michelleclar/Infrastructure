package utils

import "net/url"

func ParseUrl(str string) (*url.URL, bool) {
	if str == "" {
		return nil, false
	}
	Url, err := url.Parse(str)
	if err != nil {
		return nil, false
	}
	return Url, true
}
