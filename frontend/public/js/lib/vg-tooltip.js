"use strict";

(function() {
  // by default, delay showing tooltip for 100 ms
  var DELAY = 50;
  var tooltipPromise = undefined;
  var tooltipActive = false;

  /**
  * Export API for Vega visualizations: vg.tooltip(vgView, options)
  * options can specify whether to show all fields or to show only custom fields
  * It can also provide custom title and format for fields
  */
  window.vg = window.vg || {};
  window.vg.tooltip = function(vgView, options) {
    if (!options) {
      options = {};
    }

    // initialize tooltip with item data and options on mouse over
    vgView.on("mouseover.tooltipInit", function(event, item) {
      if (shouldShowTooltip(item)) {
        // clear existing promise because mouse can only point at one thing at a time
        cancelPromise();

        tooltipPromise = window.setTimeout(function() {
          init(event, item, options);
        }, options.delay || DELAY);
      }
    });

    // update tooltip position on mouse move
    // (important for large marks e.g. bars)
    vgView.on("mousemove.tooltipUpdate", function(event, item) {
      if (shouldShowTooltip(item) && tooltipActive) {
        update(event, item, options);
      }
    });

    // clear tooltip on mouse out
    vgView.on("mouseout.tooltipClear", function(event, item) {
      if (shouldShowTooltip(item)) {
        cancelPromise();

        if (tooltipActive) {
          clear(event, item, options);
        }
      }
    });

    return {
      destroy: function() {
        // remove event listeners
        vgView.off("mouseover.tooltipInit");
        vgView.off("mousemove.tooltipUpdate");
        vgView.off("mouseout.tooltipClear");

        cancelPromise(); // clear tooltip promise
      }
    }
  };

  /**
  * Export API for Vega-Lite visualizations: vl.tooltip(vgView, vlSpec, options)
  * options can specify whether to show all fields or to show only custom fields
  * It can also provide custom title and format for fields
  * options can be supplemented by vlSpec
  */
  window.vl = window.vl || {};
  window.vl.tooltip = function(vgView, vlSpec, options) {
    if (!options) {
      options = {};
    }

    options = supplementOptions(options, vlSpec);

    // initialize tooltip with item data and options on mouse over
    vgView.on("mouseover.tooltipInit", function(event, item) {
      if (shouldShowTooltip(item)) {
        // clear existing promise because mouse can only point at one thing at a time
        cancelPromise();

        // make a new promise with time delay for tooltip
        tooltipPromise = window.setTimeout(function() {
          init(event, item, options);
        }, options.delay || DELAY);
      }
    });

    // update tooltip position on mouse move
    // (important for large marks e.g. bars)
    vgView.on("mousemove.tooltipUpdate", function(event, item) {
      if (shouldShowTooltip(item) && tooltipActive) {
        update(event, item, options);
      }
    });

    // clear tooltip on mouse out
    vgView.on("mouseout.tooltipClear", function(event, item) {
      if (shouldShowTooltip(item)) {
        cancelPromise();

        if (tooltipActive) {
          clear(event, item, options);
        }
      }
    });

    return {
      destroy: function() {
        // remove event listeners
        vgView.off("mouseover.tooltipInit");
        vgView.off("mousemove.tooltipUpdate");
        vgView.off("mouseout.tooltipClear");

        cancelPromise(); // clear tooltip promise
      }
    }
  };

  /* Cancel tooltip promise */
  function cancelPromise() {
    /* We don't check if tooltipPromise is valid because passing
     an invalid ID to clearTimeout does not have any effect
     (and doesn't throw an exception). */
    window.clearTimeout(tooltipPromise);
    tooltipPromise = undefined;
  }

  /* Mapping from fieldDef.type to formatType */
  var formatTypeMap = {
    "quantitative": "number",
    "temporal": "time",
    "ordinal": undefined,
    "nominal": undefined
  }

  /**
  * (Vega-Lite only) Supplement options with vlSpec
  *
  * @param options - user-provided options
  * @param vlSpec - vega-lite spec
  * @return the vlSpec-supplemented options object
  *
  * if options.showAllFields is true or undefined, vlSpec will supplement
  * options.fields with all fields in the spec
  * if options.showAllFields is false, vlSpec will only supplement existing fields
  * in options.fields
  */
  function supplementOptions(options, vlSpec) {
    // fields to be supplemented by vlSpec
    var supplementedFields = [];

    // if showAllFields is true or undefined, supplement all fields in vlSpec
    if (options.showAllFields !== false) {
      vl.spec.fieldDefs(vlSpec).forEach(function(fieldDef){
        // get a fieldOption in options that matches the fieldDef
        var fieldOption = getFieldOption(options.fields, fieldDef);

        // supplement the fieldOption with fieldDef and config
        var supplementedFieldOption = supplementFieldOption(fieldOption, fieldDef, vlSpec);

        supplementedFields.push(supplementedFieldOption);
      });
    }
    // if showAllFields is false, only supplement existing fields in options.fields
    else {
      if (options.fields && options.fields.length > 0) {
        options.fields.forEach(function(fieldOption) {
          // get the fieldDef in vlSpec that matches the fieldOption
          var fieldDef = getFieldDef(vl.spec.fieldDefs(vlSpec), fieldOption);

          // supplement the fieldOption with fieldDef and config
          var supplementedFieldOption = supplementFieldOption(fieldOption, fieldDef, vlSpec);

          supplementedFields.push(supplementedFieldOption);
        })
      }
    }

    options.fields = supplementedFields;

    return options;
  }

  /**
  * Find a fieldOption in fieldOptions that matches a fieldDef
  *
  * @param {Object[]} fieldOptionss - a list of field options (i.e. options.fields[])
  * @param {Object} fieldDef - from vlSpec
  * @return the matching fieldOption, or undefined if no match was found
  *
  * If the fieldDef is aggregated, find a fieldOption that matches the field name and
  * the aggregation of the fieldDef.
  * If the fieldDef is not aggregated, find a fieldOption that matches the field name.
  */
  function getFieldOption(fieldOptions, fieldDef) {
    if (!fieldDef || !fieldOptions || fieldOptions.length <= 0) return;

    // if aggregate, match field name and aggregate operation
    if (fieldDef.aggregate) {
      // try find the perfect match: field name equals, aggregate operation equals
      for (var i = 0; i < fieldOptions.length; i++) {
        var fieldOption = fieldOptions[i];
        if (fieldOption.field === fieldDef.field && fieldOption.aggregate === fieldDef.aggregate) {
          return fieldOption;
        }
      }

      // try find the second-best match: field name equals, field.aggregate is not specified
      for (var i = 0; i < fieldOptions.length; i++) {
        var fieldOption = fieldOptions[i];
        if (fieldOption.field === fieldDef.field && !fieldOption.aggregate) {
          return fieldOption;
        }
      }

      // return undefined if no match was found
      return;
    }
    // if not aggregate, just match field name
    else {
      for (var i = 0; i < fieldOptions.length; i++) {
        var fieldOption = fieldOptions[i];
        if (fieldOption.field === fieldDef.field) {
          return fieldOption;
        }
      }

      // return undefined if no match was found
      return;
    }
  }

  /**
  * Find a fieldDef that matches a fieldOption
  *
  * @param {Object[]} fieldDefs - array of fieldDefs from vlSpec
  * @param {Object} fieldOption - a field option (a member in options.fields[])
  * @return the matching fieldDef, or undefined if no match was found
  *
  * A matching fieldDef should have the same field name as fieldOption.
  * If the matching fieldDef is aggregated, the aggregation should not contradict
  * with that of the fieldOption.
  */
  function getFieldDef(fieldDefs, fieldOption) {
    if (!fieldOption || !fieldOption.field || !fieldDefs) return;

    // field name should match, aggregation should not disagree
    for (var i = 0; i < fieldDefs.length; i++) {
      var fieldDef = fieldDefs[i];
      if (fieldDef.field === fieldOption.field) {
        if (fieldDef.aggregate) {
          if (fieldDef.aggregate === fieldOption.aggregate || !fieldOption.aggregate) {
            return fieldDef;
          }
        }
        else {
          return fieldDef;
        }
      }
    }

    // return undefined if no match was found
    return;
  }

  /**
  * Supplement a fieldOption (from options.fields[]) with a fieldDef, config
  * (which provides timeFormat, numberFormat, countTitle)
  * Either fieldOption or fieldDef can be undefined, but they cannot both be undefined.
  * config (and its members timeFormat, numberFormat and countTitle) can be undefined.
  * @return the supplemented fieldOption, or undefined on error
  */
  function supplementFieldOption(fieldOption, fieldDef, vlSpec) {
    // many specs don't have config
    var config = vl.util.extend({}, vlSpec.config);

    // at least one of fieldOption and fieldDef should exist
    if (!fieldOption && !fieldDef) {
      console.error("[Tooltip] Cannot supplement a field when field and fieldDef are both empty.");
      return;
    }

    // if either one of fieldOption and fieldDef is undefined, make it an empty object
    if (!fieldOption && fieldDef) fieldOption = {};
    if (fieldOption && !fieldDef) fieldDef = {};

    // the supplemented field option
    var supplementedFieldOption = {};

    // supplement a user-provided field name with underscore prefixes and suffixes to
    // match the field names in item.datum
    // for aggregation, this will add prefix "mean_" etc.
    // for timeUnit, this will add prefix "yearmonth_" etc.
    // for bin, this will add prefix "bin_" and suffix "_start". Later we will replace "_start" with "_range".
    supplementedFieldOption.field = fieldDef.field ?
      vl.fieldDef.field(fieldDef) : fieldOption.field;

    // If a fieldDef is a (TIMEUNIT)T, we check if the original T is present in the vlSpec.
    // If only (TIMEUNIT)T is present in vlSpec, we set `removeOriginalTemporalField` to T,
    // which will cause function removeDuplicateTimeFields() to remove T and only keep (TIMEUNIT)T
    // in item data.
    // If both (TIMEUNIT)T and T are in vlSpec, we set `removeOriginalTemporalField` to undefined,
    // which will leave both T and (TIMEUNIT)T in item data.
    // Note: user should never have to provide this boolean in options
    if (fieldDef.type === "temporal" && fieldDef.timeUnit) {
      // in most cases, if it's a (TIMEUNIT)T, we remove original T
      var originalTemporalField = fieldDef.field;
      supplementedFieldOption.removeOriginalTemporalField = originalTemporalField;

      // handle corner case: if T is present in vlSpec, then we keep both T and (TIMEUNIT)T
      var fieldDefs = vl.spec.fieldDefs(vlSpec);
      for (var i = 0; i < fieldDefs.length; i++) {
        if (fieldDefs[i].field === originalTemporalField && !fieldDefs[i].timeUnit) {
          supplementedFieldOption.removeOriginalTemporalField = undefined;
          break;
        }
      }

    }

    // supplement title
    if (!config.countTitle) config.countTitle = vl.config.defaultConfig.countTitle; // use vl default countTitle
    supplementedFieldOption.title = fieldOption.title ?
      fieldOption.title : vl.fieldDef.title(fieldDef, config);

    // supplement formatType
    supplementedFieldOption.formatType = fieldOption.formatType ?
      fieldOption.formatType : formatTypeMap[fieldDef.type];

    // supplement format
    if (fieldOption.format) {
      supplementedFieldOption.format = fieldOption.format;
    }
    // when user doesn't provide format, supplement format using timeUnit, timeFormat, and numberFormat
    else {
      switch (supplementedFieldOption.formatType) {
        case "time":
          supplementedFieldOption.format = fieldDef.timeUnit ?
            // TODO(zening): use template for all time fields, to be consistent with Vega-Lite
            vl.timeUnit.template(fieldDef.timeUnit, "", false).match(/time:'[%-a-z]*'/i)[0].split("'")[1]
              : config.timeFormat || vl.config.defaultConfig.timeFormat;
          break;
        case "number":
          supplementedFieldOption.format = config.numberFormat;
          break;
        case "string":
        default:
      }
    }

    // supplement bin from fieldDef, user should never have to provide bin in options
    if (fieldDef.bin) {
      supplementedFieldOption.field = supplementedFieldOption.field.replace("_start", "_range"); // replace suffix
      supplementedFieldOption.bin = true;
      supplementedFieldOption.formatType = "string"; // we show bin range as string (e.g. "5-10")
    }

  return supplementedFieldOption;
  }


  /* Initialize tooltip with data */
  function init(event, item, options) {
    // get tooltip HTML placeholder
    var tooltipPlaceholder = getTooltipPlaceholder();

    // prepare data for tooltip
    var tooltipData = getTooltipData(item, options);
    if (!tooltipData || tooltipData.length === 0) return;

    // bind data to tooltip HTML placeholder
    bindData(tooltipPlaceholder, tooltipData);

    updatePosition(event, options);
    updateColorTheme(options);
    d3.select("#vis-tooltip").style("visibility", "visible");
    tooltipActive = true;

    // invoke user-provided callback
    if (options.onAppear) {
      options.onAppear(event, item);
    }
  }

  /* Update tooltip position on mousemove */
  function update(event, item, options) {
    updatePosition(event, options);

    // invoke user-provided callback
    if (options.onMove) {
      options.onMove(event, item);
    }
  }

  /* Clear tooltip */
  function clear(event, item, options) {
    // visibility hidden instead of display none
    // because we need computed tooltip width and height to best position it
    d3.select("#vis-tooltip").style("visibility", "hidden");

    tooltipActive = false;
    clearData();
    clearColorTheme();
    clearPosition();

    // invoke user-provided callback
    if (options.onDisappear) {
      options.onDisappear(event, item);
    }
  }


  /* Decide if a scenegraph item deserves tooltip */
  function shouldShowTooltip (item) {
    // no data, no show
    if (!item || !item.datum) return false;

    // (small multiples) avoid showing tooltip for a facet's background
    if (item.datum._facetID) return false;

    // avoid showing tooltip for axis title and labels
    if (!item.datum._id) return false;

    // custom option to hide tooltip
    if (item.datum.hidetooltip) return false;

    return true;
  }

  /**
  * Prepare data for the tooltip
  * @return An array of tooltip data [{ title: ..., value: ...}]
  */
  function getTooltipData(item, options) {
    // this array will be bind to the tooltip element
    var tooltipData;

    var itemData = d3.map(item.datum);

    // TODO(zening): find more keys which we should remove from data (#35)
    var removeKeys = [
      "_id", "_prev",
      "count_start", "count_end",
      "layout_start", "layout_mid", "layout_end", "layout_path", "layout_x", "layout_y"
    ];
    removeFields(itemData, removeKeys);

    // remove duplicate time fields (if any)
    removeDuplicateTimeFields(itemData, options.fields);

    // combine multiple rows of a binned field into a single row
    combineBinFields(itemData, options.fields);

    // TODO(zening): use Vega-Lite layering to support tooltip on line and area charts (#1)
    dropFieldsForLineArea(item.mark.marktype, itemData);

    if ( options.showAllFields !== false ) {
      tooltipData = prepareAllFieldsData(itemData, options);
    }
    else {
      tooltipData = prepareCustomFieldsData(itemData, options);
    }

    return tooltipData;
  }


  /**
  * Prepare custom fields data for tooltip. This function foramts
  * field titles and values and returns an array of formatted fields.
  *
  * @param {d3.map} itemData - a map of item.datum
  * #param {Object} options - user-provided options
  * @return An array of formatted fields specified by options [{ title: ..., value: ...}]
  */
  function prepareCustomFieldsData(itemData, options) {
    var tooltipData = [];

    options.fields.forEach(function(fieldOption) {
      // prepare field title
      var title = fieldOption.title ? fieldOption.title : fieldOption.field;

      // get (raw) field value
      var value = getValue(itemData, fieldOption.field);
      if (value === undefined) return;

      // format value
      var formattedValue = customFormat(value, fieldOption.formatType, fieldOption.format) || autoFormat(value);

      // add formatted data to tooltipData
      tooltipData.push({title: title, value: formattedValue});

    });

    return tooltipData;
  }

  /**
  * Get a field value from a data map.
  * @param {d3.map} itemData - a map of item.datum
  * @param {string} field - the name of the field. It can contain "." to specify
  * that the field is not a direct child of item.datum
  * @return the field value on success, undefined otherwise
  */
  // TODO(zening): Mute "Cannot find field" warnings for composite vis (issue #39)
  function getValue(itemData, field) {
    var value;

    var accessors = field.split('.');

    // get the first accessor and remove it from the array
    var firstAccessor = accessors[0];
    accessors.shift();

    if (itemData.has(firstAccessor)) {
      value = itemData.get(firstAccessor);

      // if we still have accessors, use them to get the value
      accessors.forEach(function(a) {
        if (value[a]) {
          value = value[a];
        }
      });
    }

    if (value === undefined) {
      console.warn("[Tooltip] Cannot find field " + field + " in data.");
      return;
    }
    else {
      return value;
    }

  }


  /**
  * Prepare data for all fields in itemData for tooltip. This function
  * formats field titles and values and returns an array of formatted fields.
  *
  * @param {d3.map} itemData - a map of item.datum
  * @param {Object} options - user-provided options
  * @return All fields in itemData, formatted, in the form of an array: [{ title: ..., value: ...}]
  *
  * Please note that this function expects itemData to be simple {field:value} pairs.
  * It will not try to parse value if it is an object. If value is an object, please
  * use prepareCustomFieldsData() instead.
  */
  function prepareAllFieldsData(itemData, options) {
    var tooltipData = [];

    // here, fieldOptions still provides format
    var fieldOptions = d3.map(options.fields, function(d) { return d.field; });

    itemData.forEach(function(field, value) {
      // prepare title
      var title;
      if (fieldOptions.has(field) && fieldOptions.get(field).title) {
        title = fieldOptions.get(field).title;
      }
      else {
        title = field;
      }

      // format value
      if (fieldOptions.has(field)) {
        var formatType = fieldOptions.get(field).formatType;
        var format = fieldOptions.get(field).format;
      }
      var formattedValue = customFormat(value, formatType, format) || autoFormat(value);

      // add formatted data to tooltipData
      tooltipData.push({title: title, value: formattedValue});
    });

    return tooltipData;
  }

  /**
  * Remove multiple fields from a tooltip data map, using removeKeys
  *
  * Certain meta data fields (e.g. "_id", "_prev") should be hidden in the tooltip
  * by default. This function can be used to remove these fields from tooltip data.
  * @param {d3.map} dataMap - the data map that contains tooltip data.
  * @param {string[]} removeKeys - the fields that should be removed from dataMap.
  */
  function removeFields(dataMap, removeKeys) {
    removeKeys.forEach(function(key) {
      dataMap.remove(key);
    })
  }

  /**
   * When a temporal field has timeUnit, itemData will give us duplicated fields
   * (e.g., Year and YEAR(Year)). In tooltip want to display the field WITH the
   * timeUnit and remove the field that doesn't have timeUnit.
   */
  function removeDuplicateTimeFields(itemData, optFields) {
    if (!optFields) return;

    optFields.forEach(function(optField) {
      if (optField.removeOriginalTemporalField) {
        removeFields(itemData, [optField.removeOriginalTemporalField]);
      }
    });
  }

  /**
  * Combine multiple binned fields in itemData into one field. The value of the field
  * is a string that describes the bin range.
  *
  * @param {d3.map} itemData - a map of item.datum
  * @param {Object[]} fieldOptions - a list of field options (i.e. options.fields[])
  * @return itemData with combined bin fields
  */
  function combineBinFields(itemData, fieldOptions) {
    if (!fieldOptions) return;

    fieldOptions.forEach(function(fieldOption) {
      if (fieldOption.bin === true) {

        // get binned field names
        var bin_field_range = fieldOption.field;
        var bin_field_start = bin_field_range.replace('_range', '_start');
        var bin_field_mid = bin_field_range.replace('_range', '_mid');
        var bin_field_end = bin_field_range.replace('_range', '_end');

        // use start value and end value to compute range
        // save the computed range in bin_field_start
        var startValue = itemData.get(bin_field_start);
        var endValue = itemData.get(bin_field_end);
        if ((startValue !== undefined) && (endValue !== undefined)) {
          var range = startValue + '-' + endValue;
          itemData.set(bin_field_range, range);
        }

        // remove bin_field_mid, bin_field_end, and bin_field_range from itemData
        var binRemoveKeys = [];
        binRemoveKeys.push(bin_field_start, bin_field_mid, bin_field_end);
        removeFields(itemData, binRemoveKeys);
      }
    });

    return itemData;
  }

  /**
  * Drop fields for line and area marks.
  *
  * Lines and areas are defined by a series of datum. We overlay point marks
  * on top of lines and areas to allow tooltip to show all data in the series.
  * For the line marks and area marks underneath, we only show nominal fields
  * in tooltip. This is because line / area marks only give us the last datum
  * in their series. It only make sense to show the nominal fields (e.g., symbol
  * = APPL, AMZN, GOOG, IBM, MSFT) because these fields don't tend to change along
  * the line / area border.
  */
  function dropFieldsForLineArea(marktype, itemData) {
    if (marktype === "line" || marktype === "area") {
      var quanKeys = [];
      itemData.forEach(function(field, value) {
        switch (dl.type(value)) {
          case "number":
          case "date":
            quanKeys.push(field);
            break;
          case "boolean":
          case "string":
        }
      });
      removeFields(itemData, quanKeys);
    }
  }

  /**
  * Format value using formatType and format
  * @param value - a field value to be formatted
  * @param formatType - the foramtType can be: "time", "number", or "string"
  * @param format - a d3 time format specifier, or a d3 number format specifier, or undefined
  * @return the formatted value, or undefined if value or formatType is missing
  */
  function customFormat(value, formatType, format) {
    if (value === undefined || value === null) return;
    if (!formatType) return;

    switch (formatType) {
      case "time":
        return format ? dl.format.time(format)(value) : dl.format.auto.time()(value);
      case "number":
        return format ? dl.format.number(format)(value) : dl.format.auto.number()(value);
      case "string":
      default:
        return value;
    }
  }

  /**
  * Automatically format a time, number or string value
  * @return the formatted time, number or string value
  */
  function autoFormat(value) {
    switch (dl.type(value)) {
      case "date":
        return dl.format.auto.time()(value);
      case "number":
        return dl.format.auto.number()(value);
      case "boolean":
      case "string":
      default:
        return value;
    }
  }


  /**
  * Get the tooltip HTML placeholder by id selector "#vis-tooltip"
  * If none exists, create a placeholder.
  * @returns the HTML placeholder for tooltip
  */
  function getTooltipPlaceholder() {
    var tooltipPlaceholder;

    if (d3.select("#vis-tooltip").empty()) {
      tooltipPlaceholder = d3.select("body").append("div")
          .attr("id", "vis-tooltip")
          .attr("class", "vg-tooltip");
    }
    else {
      tooltipPlaceholder = d3.select("#vis-tooltip");
    }

    return tooltipPlaceholder;
  }

  /**
  * Bind tooltipData to the tooltip placeholder
  */
  function bindData(tooltipPlaceholder, tooltipData) {
    tooltipPlaceholder.selectAll("table").remove();
    var tooltipRows = tooltipPlaceholder.append("table").selectAll(".tooltip-row")
      .data(tooltipData);

    tooltipRows.exit().remove();

    var row = tooltipRows.enter().append("tr")
      .attr("class", "tooltip-row");
    row.append("td").attr("class", "key").text(function(d) { return d.title + ":"; });
    row.append("td").attr("class", "value").text(function(d) { return d.value; });
  }

  /**
  * Clear tooltip data
  */
  function clearData() {
    d3.select("#vis-tooltip").selectAll(".tooltip-row").data([])
      .exit().remove();
  }

  /**
  * Update tooltip position
  * Default position is 10px right of and 10px below the cursor. This can be
  * overwritten by options.offset
  */
  function updatePosition(event, options) {
    // determine x and y offsets, defaults are 10px
    var offsetX = 10;
    var offsetY = 10;
    if (options && options.offset && (options.offset.x !== undefined) && (options.offset.x !== null)) {
      offsetX = options.offset.x;
    }
    if (options && options.offset && (options.offset.y !== undefined) && (options.offset.y !== null)) {
      offsetY = options.offset.y;
    }

    d3.select("#vis-tooltip")
      .style("top", function() {
        // by default: put tooltip 10px below cursor
        // if tooltip is close to the bottom of the window, put tooltip 10px above cursor
        var tooltipHeight = this.getBoundingClientRect().height;
        if (event.clientY + tooltipHeight + offsetY < window.innerHeight) {
          return "" + (event.clientY + offsetY) + "px";
        } else {
          return "" + (event.clientY - tooltipHeight - offsetY) + "px";
        }
      })
      .style("left", function() {
        // by default: put tooltip 10px to the right of cursor
        // if tooltip is close to the right edge of the window, put tooltip 10 px to the left of cursor
        var tooltipWidth = this.getBoundingClientRect().width;
        if (event.clientX + tooltipWidth + offsetX < window.innerWidth) {
          return "" + (event.clientX + offsetX) + "px";
        } else {
          return "" + (event.clientX - tooltipWidth - offsetX) + "px";
        }
      });
  }

  /* Clear tooltip position */
  function clearPosition() {
    d3.select("#vis-tooltip")
      .style("top", "-9999px")
      .style("left", "-9999px");
  }

  /**
  * Update tooltip color theme according to options.colorTheme
  *
  * If colorTheme === "dark", apply dark theme to tooltip.
  * Otherwise apply light color theme.
  */
  function updateColorTheme(options) {
    clearColorTheme();

    if (options && options.colorTheme === "dark") {
      d3.select("#vis-tooltip").classed("dark-theme", true);
    }
    else {
      d3.select("#vis-tooltip").classed("light-theme", true);
    }
  }

  /* Clear color themes */
  function clearColorTheme() {
    d3.select("#vis-tooltip").classed("dark-theme light-theme", false);
  }


}());
