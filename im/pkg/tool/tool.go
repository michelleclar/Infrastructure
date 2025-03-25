package tool

import "reflect"

// ListToMap interface{} change to map[string]interface{}
// interface{} data is []interface{}
func ListToMap(list interface{}, key string) map[string]interface{} {
	res := make(map[string]interface{})
	arr := ToSlice(list)
	for _, row := range arr {
		immutable := reflect.ValueOf(row)
		val := immutable.FieldByName(key).String()
		res[val] = row
	}
	return res
}

// ToSlice interface{} change to []interface{}
func ToSlice(arr interface{}) []interface{} {
	ret := make([]interface{}, 0)
	v := reflect.ValueOf(arr)
	if v.Kind() != reflect.Slice {
		ret = append(ret, arr)
		return ret
	}
	l := v.Len()
	for i := 0; i < l; i++ {
		ret = append(ret, v.Index(i).Interface())
	}
	return ret
}
