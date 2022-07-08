import Source, {SourceType} from "./Source";
import AccessesSource from "./AccessesSource";
import TranslationSource from "./TranslationSource";

export abstract class SourceFactory {
    static getSource(source: any) : Source {
        switch (source.type) {
            case SourceType.ACCESSES:
                return new AccessesSource(source);
            case SourceType.IDTOENTITIY:
                return new TranslationSource(source);
            default:
                throw new Error('Type ' + source.type + ' unknown.');
        }
    }

    static getSources(source: any[]) : Source[] | null {
        if (source === null)
            return null;
        return source.map((source:any) => this.getSource(source))
    }
}