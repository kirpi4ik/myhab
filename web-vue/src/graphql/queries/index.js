import gql from "graphql-tag";

export * from './ports';
export * from './peripherals';
export * from './zones';
export * from './cables';
export * from './devices';
export * from './configurations';
export * from './users';

export const NAV_BREADCRUMB = gql`
    query navigation($zoneUid:String){
        navigation {
            breadcrumb(zoneUid:$zoneUid) {
                name
                zoneUid
            }
        }
    }
`;

export const PUSH_EVENT = gql`
    mutation pushEvent($input: EventDatInput){
        pushEvent(input:$input){
            p0
        }
    }
`;