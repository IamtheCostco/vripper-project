import {PostState} from '../domain/post-state.model';
import {Subscription} from 'rxjs';
import {WsConnectionService} from '../ws-connection.service';
import {GridOptions, RowNode} from 'ag-grid-community';
import {NgZone} from '@angular/core';

export class PostsDataSource {
  constructor(private ws: WsConnectionService, private gridOptions: GridOptions, private zone: NgZone) {
  }

  subscriptions: Subscription[] = [];

  connect() {
    this.subscriptions.push(this.ws.posts$.subscribe((e: PostState[]) => {
      this.zone.run(() => {
        const toAdd = [];
        const toUpdate = [];
        e.forEach(v => {
          if (this.gridOptions.api.getRowNode(v.postId) == null) {
            toAdd.push(v);
          } else {
            toUpdate.push(v);
          }
        });
        this.gridOptions.api.applyTransaction({update: toUpdate, add: toAdd});
      });
    }));

    this.subscriptions.push(this.ws.postsRemove$.subscribe((e: string[]) => {
      this.zone.run(() => {
        const toRemove = [];
        e.forEach(v => {
          const rowNode: RowNode = this.gridOptions.api.getRowNode(v);
          if (rowNode != null) {
            toRemove.push(rowNode.data);
          }
          return;
        });
        this.gridOptions.api.applyTransaction({remove: toRemove});
      });
    }));
  }

  disconnect() {
    console.log('Disconnecting from posts datasource');
    this.subscriptions.forEach(e => e.unsubscribe());
  }
}
