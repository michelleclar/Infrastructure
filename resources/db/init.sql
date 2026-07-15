-- module

-- member
CREATE
OR REPLACE FUNCTION hamming_distance(a BIGINT, b BIGINT)
RETURNS INT AS $$
BEGIN
RETURN bit_count(a # b); -- XOR 再计数
END;
$$
LANGUAGE plpgsql IMMUTABLE;
SELECT *
FROM embedding_cache
WHERE hamming_distance(simhash, :targetSimHash) <= 3;
