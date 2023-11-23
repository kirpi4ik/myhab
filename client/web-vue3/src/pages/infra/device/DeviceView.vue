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
        <div class="text-h4 text-secondary">Device details: {{ viewItem.code }}</div>
        <q-btn outline round color="amber-8" icon="mdi-pencil" :to="uri +'/'+ $route.params.idPrimary+'/edit'"
               style="margin-left: 5px"/>
        <q-btn outline round color="amber-8" icon="mdi-view-list"
               :to="'/admin/configurations/'+ $route.params.idPrimary+'?type=DEVICE'"
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
              <q-btn icon="delete" @click.stop="removeItem(props.row)"/>
            </q-td>
          </template>
        </q-table>
        <br/>
        <q-btn icon="add" color="positive" label="Add port" @click="newPort"/>

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
  </q-page>
</template>

<script>
import {defineComponent, onMounted, ref} from "vue";
import {CONFIGURATION_REMOVE_CONFIG, DEVICE_GET_BY_ID_CHILDS, PORT_DELETE_BY_ID} from "@/graphql/queries";
import {useApolloClient} from "@vue/apollo-composable";
import {useRoute, useRouter} from "vue-router/dist/vue-router";
import {useQuasar} from "quasar";

export default defineComponent({
  name: 'DeviceView',
  setup() {
    const uri = '/admin/devices'
    const $q = useQuasar()
    const viewItem = ref()
    const loading = ref(false)
    const {client} = useApolloClient();
    const router = useRouter();
    const route = useRoute();

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
    const removeItem = (toDelete) => {
      $q.dialog({
        title: 'Remove',
        message: `Doriti sa stergeti portul : ${toDelete.name} ?`,
        ok: {
          push: true,
          color: 'negative',
          label: "Remove"
        },
        cancel: {
          push: true,
          color: 'green'
        }
      }).onOk(() => {
        client.mutate({
          mutation: PORT_DELETE_BY_ID,
          variables: {id: toDelete.id},
        }).then(response => {
          fetchData()
          if (response.data.devicePortDelete.success) {
            $q.notify({
              color: 'positive',
              message: 'Port removed'
            })
          } else {
            $q.notify({
              color: 'negative',
              message: 'Failed to remove'
            })
          }
        });
      })
    }
    onMounted(() => {
      fetchData()
    })
    return {
      uri,
      fetchData,
      viewItem,
      removeItem,
      portColumns,
      onRowClick: (evt, row) => {
        if (evt.target.nodeName === 'TD' || evt.target.nodeName === 'DIV') {
          router.push({path: `/admin/ports/${row.id}/view`})
        }
      },
      newPort: () => {
        router.push({path: `/admin/ports/new`, query: {deviceId: route.params.idPrimary}})
      }
    }
  }
});
</script>
