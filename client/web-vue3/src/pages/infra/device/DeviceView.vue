<template>
  <q-page padding>
    <q-card class="my-card" v-if="viewItem">
      <q-card-section>
        <q-avatar size="103px" class="absolute-center shadow-10">
          <q-icon name="mdi-devices"/>
        </q-avatar>
        <q-btn flat color="secondary" @click="$router.go(-1)" align="right" label="Back" icon="mdi-arrow-left"/>
      </q-card-section>
      <q-card-section class="row">
        <div class="text-h4 text-secondary">Device details: {{viewItem.code}}</div>
        <q-btn outline round color="amber-8" icon="mdi-pencil" :to="uri +'/'+ $route.params.idPrimary+'/edit'"
               style="margin-left: 5px"/>
        <q-btn outline round color="amber-8" icon="mdi-view-list" :to="'/admin/configurations/'+ $route.params.idPrimary+'?type=DEVICE'"
               style="margin-left: 5px"/>
      </q-card-section>
      <q-item>
        <q-item-section>
          <q-item-label>Code</q-item-label>
          <q-item-label caption>{{ viewItem.code }}</q-item-label>
        </q-item-section>
      </q-item>
      <q-item>
        <q-item-section>
          <q-item-label>Model</q-item-label>
          <q-item-label caption>{{ viewItem.model }}</q-item-label>
        </q-item-section>
      </q-item>
      <q-item>
        <q-item-section>
          <q-item-label>Name</q-item-label>
          <q-item-label caption>{{ viewItem.name }}</q-item-label>
        </q-item-section>
      </q-item>
      <q-item>
        <q-item-section>
          <q-item-label>Description</q-item-label>
          <q-item-label caption>{{ viewItem.description }}</q-item-label>
        </q-item-section>
      </q-item>
      <q-item v-if="viewItem.type">
        <q-item-section>
          <q-item-label>Type</q-item-label>
          <q-item-label caption>{{ viewItem.type.name }}</q-item-label>
        </q-item-section>
      </q-item>
      <div class="q-pa-md">
        <q-table
          title="Connected ports"
          :rows="viewItem.ports"
          :columns="portColumns"
          @row-click="onRowClick"
          row-key="id"
        >
          <template v-slot:body-cell-actions="props">
            <q-td :props="props">
              <q-btn icon="delete"  @click="confirmDelete = true; selectedPort=props.row"></q-btn>
            </q-td>
          </template>
        </q-table>
        <br/>
        <q-btn icon="add" color="positive"  label="Add port" @click="newPort"/>

      </div>

      <q-card-actions>
        <q-btn color="accent" :to="uri +'/'+ $route.params.idPrimary+'/edit'">
          Edit
        </q-btn>
        <q-btn color="info" @click="$router.go(-1)">
          Cancel
        </q-btn>
      </q-card-actions>
    </q-card>
    <q-dialog v-model="confirmDelete" transition-show="jump-up" transition-hide="jump-down">
      <q-card>
        <q-bar class="bg-deep-orange-7 text-white">
          <q-icon name="lock"/>
          <div>Warning</div>
          <q-space/>
          <q-btn dense flat icon="close" v-close-popup>
            <q-tooltip class="bg-primary">Close</q-tooltip>
          </q-btn>
        </q-bar>
        <q-card-section class="row items-center">
          <q-avatar icon="delete" color="red" text-color="white"/>
          <span class="q-ml-sm">Do you want to remove port  <b class="text-h6 text-red">{{ selectedPort.name }}</b> ?</span>
        </q-card-section>

        <q-card-actions align="right">
          <q-btn flat label="Delete" color="red" v-close-popup
                 @click="onDelete();confirmDelete=false"/>
          <q-btn flat label="Cancel" color="positive" v-close-popup/>
        </q-card-actions>
      </q-card>
    </q-dialog>
  </q-page>
</template>

<script>
import {defineComponent, onMounted, ref} from "vue";
import {DEVICE_GET_BY_ID_CHILDS, PORT_DELETE_BY_ID} from "@/graphql/queries";
import {useApolloClient} from "@vue/apollo-composable";
import {useRoute, useRouter} from "vue-router/dist/vue-router";

export default defineComponent({
  name: 'DeviceView',
  setup() {
    const uri = '/admin/devices'
    const viewItem = ref()
    const loading = ref(false)
    const {client} = useApolloClient();
    const router = useRouter();
    const route = useRoute();
    const confirmDelete = ref(false);
    const selectedPort = ref(null);


    const portColumns = [
      {name: 'id', label: 'ID', field: 'id', align: 'left', sortable: true},
      {name: 'internalRef', label: 'RefId', field: 'internalRef', align: 'left', sortable: true},
      {name: 'name', label: 'Name', field: 'name', align: 'left', sortable: true},
      {name: 'description', label: 'Description', field: 'description', align: 'left', sortable: true},
      {name: 'actions', label: 'Actions', field: 'actions'}
    ]


    const fetchData = () => {
      loading.value = true;
      client.query({
        query: DEVICE_GET_BY_ID_CHILDS,
        variables: {id: route.params.idPrimary},
        fetchPolicy: 'network-only',
      }).then(response => {
        viewItem.value = response.data.device
        loading.value = false;
      });
    }
    onMounted(() => {
      fetchData()
    })
    return {
      uri,
      fetchData,
      viewItem,
      portColumns,
      onRowClick: (evt, row) => {
        if (evt.target.nodeName === 'TD' || evt.target.nodeName === 'DIV') {
          router.push({path: `/admin/ports/${row.id}/view`})
        }
      },
      newPort: () => {
        router.push({path: `/admin/ports/new`, query: {deviceId: route.params.idPrimary}})
      },
      confirmDelete,
      selectedPort,
      onDelete: () => {
        client.mutate({
          mutation: PORT_DELETE_BY_ID,
          variables: {id: selectedPort.value.id},
        }).then(response => {
          fetchData()
        });
      }
    }
  }
});
</script>
