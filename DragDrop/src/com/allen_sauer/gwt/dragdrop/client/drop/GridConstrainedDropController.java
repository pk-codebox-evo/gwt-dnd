/*
 * Copyright 2007 Fred Sauer
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package com.allen_sauer.gwt.dragdrop.client.drop;

import com.google.gwt.user.client.ui.AbsolutePanel;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.user.client.ui.Widget;

import com.allen_sauer.gwt.dragdrop.client.DragContext;
import com.allen_sauer.gwt.dragdrop.client.DragEndEvent;
import com.allen_sauer.gwt.dragdrop.client.util.DOMUtil;
import com.allen_sauer.gwt.dragdrop.client.util.Location;
import com.allen_sauer.gwt.dragdrop.client.util.WidgetLocation;

import java.util.HashMap;
import java.util.Iterator;

/**
 * A {@link DropController} which constrains the placement of draggable widgets
 * the grid specified in the constructor.
 */
public class GridConstrainedDropController extends AbsolutePositionDropController {
  private static final Widget helperWidget = new SimplePanel();
  private final HashMap dropLocations = new HashMap();
  private final AbsolutePanel dropTarget;
  private int gridX;
  private int gridY;

  public GridConstrainedDropController(AbsolutePanel dropTarget, int gridX, int gridY) {
    super(dropTarget);
    this.dropTarget = dropTarget;
    this.gridX = gridX;
    this.gridY = gridY;
  }

  public void drop(Widget widget, int left, int top) {
    dropTarget.add(widget, left, top);
    constrainedWidgetMove(widget, widget);
  }

  public DragEndEvent onDrop(DragContext context) {
    for (Iterator iterator = context.selectedWidgets.iterator(); iterator.hasNext();) {
      Widget widget = (Widget) iterator.next();
      Location dropLocation = (Location) dropLocations.get(widget);
      dropTarget.add(widget, dropLocation.getLeft(), dropLocation.getTop());
    }
    dropLocations.clear();
    return makeDragEndEvent(context);
  }

  public void onMove(DragContext context) {
    constrainedWidgetMove(context.movableWidget, getPositioner());
  }

  public void onPreviewDrop(DragContext context) throws VetoDropException {
    WidgetLocation referenceLocation = new WidgetLocation(context.movableWidget, context.boundaryPanel);

    // temporarily store widget drop locations for use in onDrop()
    dropLocations.clear();

    try {
      for (Iterator iterator = context.selectedWidgets.iterator(); iterator.hasNext();) {
        Widget widget = (Widget) iterator.next();
        WidgetLocation relativeLocation = new WidgetLocation(widget, context.draggable);

        // Use helper widget to determine constrained location
        context.boundaryPanel.add(helperWidget, referenceLocation.getLeft() + relativeLocation.getLeft(),
            referenceLocation.getTop() + relativeLocation.getTop());
        helperWidget.setPixelSize(widget.getOffsetWidth(), widget.getOffsetHeight());
        Location dropLocation = getConstrainedLocation(helperWidget);
        if (dropLocation == null) {
          dropLocations.clear();
          throw new VetoDropException();
        }
        dropLocations.put(widget, dropLocation);
      }
    } finally {
      helperWidget.removeFromParent();
    }
  }

  private void constrainedWidgetMove(Widget reference, Widget widget) {
    Location location = getConstrainedLocation(reference);
    if (location != null) {
      dropTarget.add(widget, location.getLeft(), location.getTop());
    }
  }

  private Location getConstrainedLocation(Widget reference) {
    WidgetLocation referenceLocation = new WidgetLocation(reference, dropTarget);
    referenceLocation.constrain(0, 0, DOMUtil.getClientWidth(dropTarget.getElement()) - reference.getOffsetWidth(),
        DOMUtil.getClientHeight(dropTarget.getElement()) - reference.getOffsetHeight());
    return referenceLocation.newLocationSnappedToGrid(gridX, gridY);
  }
}
