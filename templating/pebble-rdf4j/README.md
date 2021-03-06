# 1 pebble-rdf4j

# 1.1 pebble
This templating module includes rdf4j related extensions for Pebble, like filtering.

# 1.2 filtering
In order to filter Json+Ld, use ```jsonld``` filter in the pebble templates. For example, to filter the ```fields```
with jsonld, use:
```html
{% block script %}
<script type="application/ld+json">{ fields | jsonld | raw }}</script>
{% endblock %}
```
The addition ```raw``` makes sure that the result is not encoded.

# 1.3 custom filter
In order to create custom Pebble filter, see: https://pebbletemplates.io/wiki/guide/extending-pebble
