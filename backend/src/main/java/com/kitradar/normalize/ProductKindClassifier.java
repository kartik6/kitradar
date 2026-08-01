package com.kitradar.normalize;

import com.kitradar.domain.ProductKind;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;

@Component
public class ProductKindClassifier {

    private static final Pattern NON_JERSEY = Pattern.compile(
            "short|sock|jacket|tracksuit|bag|mug|poster|hoodie|sweatshirt|cap\\b|scarf|ball\\b|glove|keychain|baseball shirt",
            Pattern.CASE_INSENSITIVE);

    public ProductKind classify(String title) {
        return title != null && NON_JERSEY.matcher(title).find()
                ? ProductKind.OTHER
                : ProductKind.JERSEY;
    }
}
