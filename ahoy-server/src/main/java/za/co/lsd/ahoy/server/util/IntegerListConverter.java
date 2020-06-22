package za.co.lsd.ahoy.server.util;

import javax.persistence.AttributeConverter;
import javax.persistence.Converter;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Converter
public class IntegerListConverter implements AttributeConverter<List<Integer>, String> {
	private static final String SPLIT_CHAR = ";";

	@Override
	public String convertToDatabaseColumn(List<Integer> integers) {
		if (integers == null)
			return null;

		return integers.stream()
			.map(i -> Integer.toString(i))
			.collect(Collectors.joining(SPLIT_CHAR));
	}

	@Override
	public List<Integer> convertToEntityAttribute(String dbData) {
		if (dbData == null)
			return null;

		return Arrays.stream(dbData.split(SPLIT_CHAR))
			.filter(s -> !s.isEmpty())
			.map(Integer::parseInt)
			.collect(Collectors.toList());
	}
}
